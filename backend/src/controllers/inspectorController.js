const db = require('../../config/db');
const { generarLicenciaPDF } = require('../services/pdfService');
const path = require('path');
const fs = require('fs');
const { v4: uuidv4 } = require('uuid');

// GET /api/inspector/mis-inspecciones
const misInspecciones = async (req, res) => {
  try {
    const { rows } = await db.query(
      `SELECT i.*, s.numero_expediente, s.estado, s.plano_url,
              n.ruc, n.razon_social, n.domicilio_fiscal, n.rubro, n.area_m2,
              n.rep_nombre, n.rep_telefono
       FROM inspecciones i
       JOIN solicitudes s ON i.solicitud_id = s.id
       JOIN negocios n ON s.negocio_id = n.id
       WHERE i.inspector_id = $1
       ORDER BY i.fecha_programada ASC`,
      [req.usuario.id]
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: 'Error al obtener inspecciones' });
  }
};

// POST /api/inspector/inspecciones/:id/resultado
const registrarResultado = async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const { id } = req.params;
    const { resultado, observaciones, requiereDocs } = req.body;

    if (!['conforme', 'observado'].includes(resultado))
      return res.status(400).json({ error: 'Resultado debe ser conforme u observado' });

    // Verificar que la inspección pertenece al inspector
    const { rows: insp } = await client.query(
      'SELECT * FROM inspecciones WHERE id = $1 AND inspector_id = $2',
      [id, req.usuario.id]
    );
    if (!insp[0]) return res.status(404).json({ error: 'Inspección no encontrada' });
    if (insp[0].resultado !== 'pendiente')
      return res.status(400).json({ error: 'Esta inspección ya fue registrada' });

    // Actualizar inspección
    await client.query(
      `UPDATE inspecciones SET resultado = $1, observaciones = $2, requiere_docs = $3, fecha_realizada = NOW()
       WHERE id = $4`,
      [resultado, observaciones || null, requiereDocs || false, id]
    );

    const solicitudId = insp[0].solicitud_id;
    const numInsp = insp[0].numero_inspeccion;

    if (resultado === 'conforme') {
      // APROBAR: generar licencia
      const licencia = await generarLicencia(client, solicitudId);
      await client.query(
        "UPDATE solicitudes SET estado = 'aprobado', actualizado_en = NOW() WHERE id = $1",
        [solicitudId]
      );
      await client.query(
        "INSERT INTO auditoria (usuario_id, accion, tabla, registro_id) VALUES ($1,'APROBAR_INSPECCION','inspecciones',$2)",
        [req.usuario.id, id]
      );
      await client.query('COMMIT');
      return res.json({ mensaje: 'Inspección aprobada. Licencia generada exitosamente.', licencia });
    }

    // OBSERVADO
    if (numInsp === 1) {
      // Primera inspección con observaciones: programar segunda automáticamente en 30 días hábiles
      const segundaFecha = calcularDiasHabiles(new Date(), 30);
      await client.query(
        "UPDATE solicitudes SET estado = 'observado', actualizado_en = NOW() WHERE id = $1",
        [solicitudId]
      );
      // Nota: municipalidad asignará inspector para la segunda visita
    } else {
      // Segunda inspección con observaciones: DENEGAR
      await client.query(
        "UPDATE solicitudes SET estado = 'denegado', actualizado_en = NOW() WHERE id = $1",
        [solicitudId]
      );
    }

    await client.query(
      "INSERT INTO auditoria (usuario_id, accion, tabla, registro_id) VALUES ($1,'OBSERVAR_INSPECCION','inspecciones',$2)",
      [req.usuario.id, id]
    );

    await client.query('COMMIT');
    res.json({
      mensaje: numInsp === 1
        ? 'Observaciones registradas. Se programará una segunda visita en 30 días hábiles.'
        : 'Segunda inspección con observaciones. Licencia denegada.',
      requiereDocs: requiereDocs || false
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('registrarResultado error:', err);
    res.status(500).json({ error: 'Error al registrar resultado' });
  } finally {
    client.release();
  }
};

// Genera la licencia en BD y PDF
const generarLicencia = async (client, solicitudId) => {
  const { rows: sol } = await client.query(
    `SELECT s.*, n.ruc, n.razon_social, n.nombre_comercial, n.domicilio_fiscal, n.rubro, n.area_m2
     FROM solicitudes s JOIN negocios n ON s.negocio_id = n.id
     WHERE s.id = $1`,
    [solicitudId]
  );
  const sol0 = sol[0];

  const { rows: numRows } = await client.query('SELECT generar_numero_licencia() AS num');
  const numeroLicencia = numRows[0].num;
  const { rows: resRows } = await client.query('SELECT nextval(\'seq_resolucion\') AS num');
  const numeroResolucion = `N° ${resRows[0].num}-${new Date().getFullYear()}/MPT-GDU-SGAM`;

  const codigoVerificacion = uuidv4().substring(0, 12).toUpperCase();
  const fechaEmision = new Date();
  const fechaVencimiento = new Date(fechaEmision);
  fechaVencimiento.setFullYear(fechaVencimiento.getFullYear() + 1);

  const { rows: lic } = await client.query(
    `INSERT INTO licencias
      (solicitud_id, negocio_id, numero_licencia, numero_resolucion,
       fecha_emision, fecha_vencimiento, activa, codigo_verificacion)
     VALUES ($1, $2, $3, $4, $5, $6, TRUE, $7)
     RETURNING *`,
    [solicitudId, sol0.negocio_id, numeroLicencia, numeroResolucion,
     fechaEmision, fechaVencimiento, codigoVerificacion]
  );

  // Generar PDF
  const uploadsDir = path.join(__dirname, '../../uploads/licencias');
  if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir, { recursive: true });

  const pdfPath = path.join(uploadsDir, `licencia_${numeroLicencia}.pdf`);
  await generarLicenciaPDF({
    numeroLicencia,
    numeroExpediente: sol0.numero_expediente,
    numeroResolucion,
    razonSocial: sol0.razon_social,
    nombreComercial: sol0.nombre_comercial,
    domicilioFiscal: sol0.domicilio_fiscal,
    ruc: sol0.ruc,
    areaM2: sol0.area_m2,
    rubro: sol0.rubro,
    fechaIngreso: sol0.creado_en,
    fechaEmision,
    fechaVencimiento,
    codigoPredio: '0',
    codigoVerificacion
  }, pdfPath);

  const pdfUrl = `/uploads/licencias/licencia_${numeroLicencia}.pdf`;
  await client.query('UPDATE licencias SET pdf_url = $1 WHERE id = $2', [pdfUrl, lic[0].id]);
  await client.query('UPDATE solicitudes SET fecha_vencimiento = $1 WHERE id = $2', [fechaVencimiento, solicitudId]);

  return { ...lic[0], pdfUrl };
};

const calcularDiasHabiles = (fecha, dias) => {
  let count = 0;
  const d = new Date(fecha);
  while (count < dias) {
    d.setDate(d.getDate() + 1);
    const dia = d.getDay();
    if (dia !== 0 && dia !== 6) count++;
  }
  return d;
};

module.exports = { misInspecciones, registrarResultado };
