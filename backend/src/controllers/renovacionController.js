const db = require('../../config/db');
const { generarLicenciaPDF } = require('../services/pdfService');
const path = require('path');
const fs2 = require('fs');
const { v4: uuidv4 } = require('uuid');

const misLicencias = async (req, res) => {
  try {
    const { rows } = await db.query(
      'SELECT l.*, n.ruc, n.razon_social, n.domicilio_fiscal, n.rubro, s.numero_expediente FROM licencias l JOIN negocios n ON l.negocio_id = n.id JOIN solicitudes s ON l.solicitud_id = s.id WHERE n.usuario_id = $1 ORDER BY l.creado_en DESC',
      [req.usuario.id]
    );
    res.json(rows);
  } catch (err) { res.status(500).json({ error: 'Error al obtener licencias' }); }
};

const renovarLicencia = async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { licenciaId } = req.params;
    const { numeroTarjeta, titular, mesAnio, cvv } = req.body;
    if (!numeroTarjeta || !titular || !mesAnio || !cvv)
      return res.status(400).json({ error: 'Completa todos los datos de pago' });
    const { rows } = await client.query(
      'SELECT l.*, n.usuario_id, n.ruc, n.razon_social, n.nombre_comercial, n.domicilio_fiscal, n.rubro, n.area_m2, s.numero_expediente FROM licencias l JOIN negocios n ON l.negocio_id = n.id JOIN solicitudes s ON l.solicitud_id = s.id WHERE l.id = $1',
      [licenciaId]
    );
    if (!rows[0]) return res.status(404).json({ error: 'Licencia no encontrada' });
    if (rows[0].usuario_id !== req.usuario.id) return res.status(403).json({ error: 'No autorizado' });
    const lic = rows[0];
    const { rows: numRows } = await client.query('SELECT generar_numero_licencia() AS num');
    const nuevoNumero = numRows[0].num;
    const { rows: resRows } = await client.query("SELECT nextval('seq_resolucion') AS num");
    const numeroResolucion = 'N grado ' + resRows[0].num + '-' + new Date().getFullYear() + '/MPT-GDU-SGAM';
    const codigoVerificacion = uuidv4().substring(0, 12).toUpperCase();
    const fechaEmision = new Date();
    const fechaVencimiento = new Date(fechaEmision);
    fechaVencimiento.setFullYear(fechaVencimiento.getFullYear() + 1);
    await client.query('UPDATE licencias SET activa = FALSE WHERE id = $1', [licenciaId]);
    const { rows: nuevaLic } = await client.query(
      'INSERT INTO licencias (solicitud_id, negocio_id, numero_licencia, numero_resolucion, fecha_emision, fecha_vencimiento, activa, codigo_verificacion) VALUES ($1,$2,$3,$4,$5,$6,TRUE,$7) RETURNING *',
      [lic.solicitud_id, lic.negocio_id, nuevoNumero, numeroResolucion, fechaEmision, fechaVencimiento, codigoVerificacion]
    );
    const uploadsDir = path.join(__dirname, '../../uploads/licencias');
    if (!fs2.existsSync(uploadsDir)) fs2.mkdirSync(uploadsDir, { recursive: true });
    const pdfPath = path.join(uploadsDir, 'licencia_' + nuevoNumero + '.pdf');
    await generarLicenciaPDF({
      numeroLicencia: nuevoNumero, numeroExpediente: lic.numero_expediente,
      numeroResolucion, razonSocial: lic.razon_social, nombreComercial: lic.nombre_comercial,
      domicilioFiscal: lic.domicilio_fiscal, ruc: lic.ruc, areaM2: lic.area_m2,
      rubro: lic.rubro, fechaIngreso: lic.creado_en, fechaEmision, fechaVencimiento,
      codigoPredio: '0', codigoVerificacion
    }, pdfPath);
    const pdfUrl = '/uploads/licencias/licencia_' + nuevoNumero + '.pdf';
    await client.query('UPDATE licencias SET pdf_url = $1 WHERE id = $2', [pdfUrl, nuevaLic[0].id]);
    await client.query('COMMIT');
    res.json({ mensaje: 'Licencia renovada exitosamente', licencia: { ...nuevaLic[0], pdfUrl } });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('renovar error:', err);
    res.status(500).json({ error: 'Error al renovar licencia' });
  } finally { client.release(); }
};

module.exports = { misLicencias, renovarLicencia };
