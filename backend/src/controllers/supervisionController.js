const db = require('../../config/db');

const registrarSupervision = async (req, res) => {
  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');
    const { licenciaId } = req.params;
    const { resultado, observaciones, multaAplicada, multaMonto, revocarLicencia } = req.body;
    const validos = ['conforme', 'infraccion'];
    if (!validos.includes(resultado))
      return res.status(400).json({ error: 'Resultado invalido' });
    const { rows } = await client.query(
      'SELECT * FROM licencias WHERE id = $1 AND activa = TRUE', [licenciaId]
    );
    if (!rows[0]) return res.status(404).json({ error: 'Licencia no encontrada o inactiva' });
    await client.query(
      'INSERT INTO supervisiones (licencia_id, inspector_id, fecha, resultado, observaciones, multa_aplicada, multa_monto, licencia_revocada) VALUES ($1,$2,NOW(),$3,$4,$5,$6,$7)',
      [licenciaId, req.usuario.id, resultado, observaciones||null, multaAplicada||false, multaMonto||null, revocarLicencia||false]
    );
    if (revocarLicencia) {
      await client.query('UPDATE licencias SET activa = FALSE WHERE id = $1', [licenciaId]);
    }
    await client.query('COMMIT');
    res.json({ mensaje: revocarLicencia ? 'Licencia revocada' : resultado === 'infraccion' ? 'Infraccion registrada' : 'Supervision conforme' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'Error al registrar supervision' });
  } finally { client.release(); }
};

const listarSupervisiones = async (req, res) => {
  try {
    const { rows } = await db.query(
      'SELECT sv.*, l.numero_licencia, n.razon_social, n.domicilio_fiscal, u.email as inspector_email FROM supervisiones sv JOIN licencias l ON sv.licencia_id = l.id JOIN negocios n ON l.negocio_id = n.id JOIN usuarios u ON sv.inspector_id = u.id ORDER BY sv.creado_en DESC'
    );
    res.json(rows);
  } catch (err) { res.status(500).json({ error: 'Error' }); }
};

const licenciasActivas = async (req, res) => {
  try {
    const { rows } = await db.query(
      'SELECT l.id, l.numero_licencia, l.fecha_vencimiento, l.activa, n.razon_social, n.ruc, n.domicilio_fiscal, n.rubro FROM licencias l JOIN negocios n ON l.negocio_id = n.id WHERE l.activa = TRUE ORDER BY n.razon_social'
    );
    res.json(rows);
  } catch (err) { res.status(500).json({ error: 'Error' }); }
};

module.exports = { registrarSupervision, listarSupervisiones, licenciasActivas };
