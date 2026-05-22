const db = require('../../config/db');
const { consultarRUC } = require('../services/sunatService');
// Culqi reemplazado por simulador de pago para demo académica
const { generarLicenciaPDF } = require('../services/pdfService');
const { v4: uuidv4 } = require('uuid');
const path = require('path');
const fs = require('fs');

// POST /api/solicitudes/validar-ruc
const validarRUC = async (req, res) => {
  try {
    const { ruc } = req.body;
    const resultado = await consultarRUC(ruc);
    res.json(resultado);
  } catch (err) {
    res.status(500).json({ error: 'Error al validar RUC' });
  }
};

// POST /api/solicitudes/registrar
const registrarSolicitud = async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const {
      ruc, razonSocial, nombreComercial, domicilioFiscal, rubro, areaM2,
      repNombre, repDni, repTelefono
    } = req.body;

    // Validar RUC
    const sunat = await consultarRUC(ruc);
    if (!sunat.valido) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: sunat.error });
    }

    // Verificar si ya tiene negocio registrado
    const negocioExiste = await client.query(
      'SELECT id FROM negocios WHERE usuario_id = $1',
      [req.usuario.id]
    );
    if (negocioExiste.rows[0]) {
      await client.query('ROLLBACK');
      return res.status(409).json({ error: 'Ya tienes un negocio registrado' });
    }

    // Verificar RUC único
    const rucExiste = await client.query('SELECT id FROM negocios WHERE ruc = $1', [ruc]);
    if (rucExiste.rows[0]) {
      await client.query('ROLLBACK');
      return res.status(409).json({ error: 'El RUC ya tiene un trámite registrado' });
    }

    // Crear negocio
    const negocio = await client.query(
      `INSERT INTO negocios
        (usuario_id, ruc, razon_social, nombre_comercial, domicilio_fiscal, rubro, area_m2,
         rep_nombre, rep_dni, rep_telefono, sunat_validado, sunat_validado_en)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,NOW())
       RETURNING *`,
      [req.usuario.id, ruc, razonSocial, nombreComercial, domicilioFiscal, rubro,
       areaM2 || null, repNombre, repDni, repTelefono, sunat.valido]
    );

    // Generar número de expediente
    const { rows: expRows } = await client.query('SELECT generar_expediente() AS expediente');
    const expediente = expRows[0].expediente;

    // Crear solicitud
    const solicitud = await client.query(
      `INSERT INTO solicitudes (negocio_id, numero_expediente, estado)
       VALUES ($1, $2, 'pendiente_pago')
       RETURNING *`,
      [negocio.rows[0].id, expediente]
    );

    await client.query(
      "INSERT INTO auditoria (usuario_id, accion, tabla, registro_id) VALUES ($1,'CREAR_SOLICITUD','solicitudes',$2)",
      [req.usuario.id, solicitud.rows[0].id]
    );

    await client.query('COMMIT');
    res.status(201).json({
      solicitud: solicitud.rows[0],
      negocio: negocio.rows[0],
      mensaje: 'Solicitud creada. Procede con el pago para continuar.'
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('registrarSolicitud error:', err);
    res.status(500).json({ error: 'Error al registrar solicitud' });
  } finally {
    client.release();
  }
};

// POST /api/solicitudes/:id/pagar
const procesarPagoSolicitud = async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const { id } = req.params;
    const { numeroTarjeta, titular, mesAnio, cvv } = req.body;

    // Verificar solicitud pertenece al usuario
    const { rows } = await client.query(
      `SELECT s.*, n.ruc, n.usuario_id, n.razon_social
       FROM solicitudes s JOIN negocios n ON s.negocio_id = n.id
       WHERE s.id = $1`,
      [id]
    );

    if (!rows[0]) return res.status(404).json({ error: 'Solicitud no encontrada' });
    if (rows[0].usuario_id !== req.usuario.id)
      return res.status(403).json({ error: 'No autorizado' });
    if (rows[0].pago_confirmado)
      return res.status(400).json({ error: 'El pago ya fue procesado' });

    // Validaciones básicas del formulario de pago
    if (!numeroTarjeta || !titular || !mesAnio || !cvv)
      return res.status(400).json({ error: 'Completa todos los datos de pago' });

    const numLimpio = numeroTarjeta.replace(/\s/g, '');
    if (numLimpio.length < 13 || numLimpio.length > 19)
      return res.status(400).json({ error: 'Número de tarjeta inválido' });

    // Simulador: genera un ID de transacción
    const chargeId = `SIM-${Date.now()}-${Math.random().toString(36).substring(2,8).toUpperCase()}`;

    // Actualizar solicitud
    await client.query(
      `UPDATE solicitudes SET
        pago_culqi_id = $1,
        pago_fecha = NOW(),
        pago_confirmado = TRUE,
        estado = 'documentos_pendientes',
        actualizado_en = NOW()
       WHERE id = $2`,
      [chargeId, id]
    );

    await client.query(
      "INSERT INTO auditoria (usuario_id, accion, tabla, registro_id, detalle) VALUES ($1,'PAGO','solicitudes',$2,$3)",
      [req.usuario.id, id, JSON.stringify({ chargeId: pago.chargeId, monto: 180 })]
    );

    await client.query('COMMIT');
    res.json({ mensaje: 'Pago procesado exitosamente. Ahora sube tu documentación.', chargeId });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('pago error:', err);
    res.status(500).json({ error: 'Error al procesar pago' });
  } finally {
    client.release();
  }
};

// POST /api/solicitudes/:id/subir-plano
const subirPlano = async (req, res) => {
  try {
    const { id } = req.params;
    if (!req.file) return res.status(400).json({ error: 'No se subió ningún archivo' });

    const { rows } = await db.query(
      `SELECT s.*, n.usuario_id FROM solicitudes s JOIN negocios n ON s.negocio_id = n.id WHERE s.id = $1`,
      [id]
    );
    if (!rows[0]) return res.status(404).json({ error: 'Solicitud no encontrada' });
    if (rows[0].usuario_id !== req.usuario.id) return res.status(403).json({ error: 'No autorizado' });
    // pago no requerido para demo

    const planoUrl = `/uploads/${req.file.filename}`;
    await db.query(
      `UPDATE solicitudes SET plano_url = $1, estado = 'en_validacion', plano_actualizado = FALSE, actualizado_en = NOW() WHERE id = $2`,
      [planoUrl, id]
    );

    res.json({ mensaje: 'Plano subido. Tu solicitud está en validación.', planoUrl });
  } catch (err) {
    console.error('subirPlano error:', err);
    res.status(500).json({ error: 'Error al subir plano' });
  }
};

// GET /api/solicitudes/mis-solicitudes
const misSolicitudes = async (req, res) => {
  try {
    const { rows } = await db.query(
      `SELECT s.*, n.ruc, n.razon_social, n.rubro, n.domicilio_fiscal
       FROM solicitudes s JOIN negocios n ON s.negocio_id = n.id
       WHERE n.usuario_id = $1
       ORDER BY s.creado_en DESC`,
      [req.usuario.id]
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: 'Error al obtener solicitudes' });
  }
};

// GET /api/solicitudes (municipalidad ve todas)
const todasSolicitudes = async (req, res) => {
  try {
    const { estado, page = 1 } = req.query;
    const limit = 20;
    const offset = (page - 1) * limit;

    let query = `
      SELECT s.*, n.ruc, n.razon_social, n.rubro, n.rep_nombre, n.rep_telefono
      FROM solicitudes s JOIN negocios n ON s.negocio_id = n.id
    `;
    const params = [];
    if (estado) {
      params.push(estado);
      query += ` WHERE s.estado = $${params.length}`;
    }
    query += ` ORDER BY s.creado_en DESC LIMIT ${limit} OFFSET ${offset}`;

    const { rows } = await db.query(query, params);
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: 'Error al obtener solicitudes' });
  }
};

// POST /api/solicitudes/:id/programar-inspeccion (municipalidad)
const programarInspeccion = async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { id } = req.params;
    const { inspectorId, fechaProgramada, horaProgramada } = req.body;

    const { rows: sol } = await client.query('SELECT * FROM solicitudes WHERE id = $1', [id]);
    if (!sol[0]) return res.status(404).json({ error: 'Solicitud no encontrada' });

    // Determinar número de inspección
    const { rows: insps } = await client.query(
      'SELECT COUNT(*) as total FROM inspecciones WHERE solicitud_id = $1',
      [id]
    );
    const numInspeccion = parseInt(insps[0].total) + 1;
    if (numInspeccion > 2) return res.status(400).json({ error: 'Ya se realizaron las 2 inspecciones permitidas' });

    await client.query(
      `INSERT INTO inspecciones (solicitud_id, inspector_id, numero_inspeccion, fecha_programada, hora_programada, resultado)
       VALUES ($1,$2,$3,$4,$5,'pendiente')`,
      [id, inspectorId, numInspeccion, fechaProgramada, horaProgramada || null]
    );

    const nuevoEstado = numInspeccion === 1 ? 'inspeccion_programada' : 'segunda_inspeccion_programada';
    await client.query(
      'UPDATE solicitudes SET estado = $1, actualizado_en = NOW() WHERE id = $2',
      [nuevoEstado, id]
    );

    await client.query('COMMIT');
    res.json({ mensaje: `Inspección N°${numInspeccion} programada exitosamente` });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('programarInspeccion error:', err);
    res.status(500).json({ error: 'Error al programar inspección' });
  } finally {
    client.release();
  }
};

// GET /api/licencias/:codigo/verificar (público)
const verificarLicencia = async (req, res) => {
  try {
    const { codigo } = req.params;
    const { rows } = await db.query(
      `SELECT l.*, n.ruc, n.razon_social, n.nombre_comercial, n.domicilio_fiscal, n.rubro
       FROM licencias l JOIN negocios n ON l.negocio_id = n.id
       WHERE l.codigo_verificacion = $1`,
      [codigo]
    );
    if (!rows[0]) return res.status(404).json({ error: 'Licencia no encontrada o código inválido' });
    const lic = rows[0];
    res.json({
      valida: lic.activa && new Date(lic.fecha_vencimiento) > new Date(),
      numeroLicencia: lic.numero_licencia,
      razonSocial: lic.razon_social,
      ruc: lic.ruc,
      domicilio: lic.domicilio_fiscal,
      rubro: lic.rubro,
      fechaEmision: lic.fecha_emision,
      fechaVencimiento: lic.fecha_vencimiento,
      estado: lic.activa ? 'VIGENTE' : 'REVOCADA'
    });
  } catch (err) {
    res.status(500).json({ error: 'Error al verificar licencia' });
  }
};


// POST /api/solicitudes/:id/subir-docs-corregidos
const subirDocsCorregidos = async (req, res) => {
  try {
    const { id } = req.params;
    if (!req.file) return res.status(400).json({ error: 'No se subio ningun archivo' });
    const { rows } = await db.query(
      'SELECT s.*, n.usuario_id FROM solicitudes s JOIN negocios n ON s.negocio_id = n.id WHERE s.id = $1',
      [id]
    );
    if (!rows[0]) return res.status(404).json({ error: 'Solicitud no encontrada' });
    if (rows[0].usuario_id !== req.usuario.id) return res.status(403).json({ error: 'No autorizado' });
    if (rows[0].estado !== 'observado') return res.status(400).json({ error: 'La solicitud no tiene observaciones pendientes' });
    const planoUrl = '/uploads/' + req.file.filename;
    await db.query(
      'UPDATE solicitudes SET plano_url = $1, plano_actualizado = TRUE WHERE id = $2',
      [planoUrl, id]
    );
    await db.query(
      'UPDATE inspecciones SET docs_corregidos = TRUE WHERE solicitud_id = $1 AND numero_inspeccion = 1',
      [id]
    );
    res.json({ mensaje: 'Documentos corregidos subidos correctamente', planoUrl });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error al subir documentos' });
  }
};

module.exports = {
  validarRUC, registrarSolicitud, procesarPagoSolicitud, subirPlano,
  misSolicitudes, todasSolicitudes, programarInspeccion, verificarLicencia, subirDocsCorregidos
};
