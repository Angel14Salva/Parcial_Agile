const express = require('express');
const multer = require('multer');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { autenticar, autorizar } = require('../middleware/auth');

const authCtrl = require('../controllers/authController');
const solicitudesCtrl = require('../controllers/solicitudesController');
const inspectorCtrl = require('../controllers/inspectorController');

const router = express.Router();

// ── Multer config ────────────────────────────────────────────
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, path.join(__dirname, '../../uploads/planos'));
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname);
    cb(null, `plano_${uuidv4()}${ext}`);
  }
});
const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB
  fileFilter: (req, file, cb) => {
    const allowed = ['.pdf', '.jpg', '.jpeg', '.png'];
    const ext = path.extname(file.originalname).toLowerCase();
    if (allowed.includes(ext)) cb(null, true);
    else cb(new Error('Solo se permiten PDF e imágenes'));
  }
});

// ── AUTH ─────────────────────────────────────────────────────
router.post('/auth/login', authCtrl.login);
router.post('/auth/registro', authCtrl.registro);
router.post('/auth/crear-usuario', autenticar, autorizar('municipalidad'), authCtrl.crearUsuario);
router.get('/auth/perfil', autenticar, authCtrl.perfil);

// ── SOLICITUDES (negocio) ────────────────────────────────────
router.post('/solicitudes/validar-ruc', autenticar, autorizar('negocio'), solicitudesCtrl.validarRUC);
router.post('/solicitudes/validar-dni', autenticar, autorizar('negocio'), async (req, res) => {
  const { consultarDNI } = require('../services/sunatService');
  const { dni } = req.body;
  const resultado = await consultarDNI(dni);
  res.json(resultado);
});
router.post('/solicitudes/registrar', autenticar, autorizar('negocio'), solicitudesCtrl.registrarSolicitud);
router.post('/solicitudes/:id/pagar', autenticar, autorizar('negocio'), solicitudesCtrl.procesarPagoSolicitud);
router.post('/solicitudes/:id/subir-plano', autenticar, autorizar('negocio'),
  upload.single('plano'), solicitudesCtrl.subirPlano);
router.post('/solicitudes/:id/subir-docs-corregidos', autenticar, autorizar('negocio'), upload.single('plano'), solicitudesCtrl.subirDocsCorregidos);
router.get('/solicitudes/mis-solicitudes', autenticar, autorizar('negocio'), solicitudesCtrl.misSolicitudes);

// ── LICENCIAS (descarga - negocio autenticado) ───────────────
router.get('/licencias/:solicitudId/descargar', autenticar, autorizar('negocio'), async (req, res) => {
  const db = require('../../config/db');
  try {
    const { rows } = await db.query(
      `SELECT l.pdf_url, n.usuario_id
       FROM licencias l
       JOIN solicitudes s ON l.solicitud_id = s.id
       JOIN negocios n ON s.negocio_id = n.id
       WHERE s.id = $1`,
      [req.params.solicitudId]
    );
    if (!rows[0]) return res.status(404).json({ error: 'Licencia no encontrada' });
    if (rows[0].usuario_id !== req.usuario.id) return res.status(403).json({ error: 'No autorizado' });
    const filePath = path.join(__dirname, '../..', rows[0].pdf_url);
    res.download(filePath);
  } catch (err) {
    res.status(500).json({ error: 'Error al descargar licencia' });
  }
});

// ── VERIFICACIÓN PÚBLICA ─────────────────────────────────────
router.get('/verificar/:codigo', solicitudesCtrl.verificarLicencia);

// ── INSPECTOR ────────────────────────────────────────────────
router.get('/inspector/mis-inspecciones', autenticar, autorizar('inspector'), inspectorCtrl.misInspecciones);
router.post('/inspector/inspecciones/:id/resultado', autenticar, autorizar('inspector'), inspectorCtrl.registrarResultado);

// ── MUNICIPALIDAD ────────────────────────────────────────────
router.get('/municipalidad/solicitudes', autenticar, autorizar('municipalidad'), solicitudesCtrl.todasSolicitudes);
router.post('/municipalidad/solicitudes/:id/programar-inspeccion', autenticar, autorizar('municipalidad'), solicitudesCtrl.programarInspeccion);

// Estadísticas dashboard municipalidad
router.get('/municipalidad/stats', autenticar, autorizar('municipalidad'), async (req, res) => {
  const db = require('../../config/db');
  try {
    const stats = await db.query(`
      SELECT
        COUNT(*) FILTER (WHERE estado = 'pendiente_pago') AS pendientes_pago,
        COUNT(*) FILTER (WHERE estado = 'en_validacion') AS en_validacion,
        COUNT(*) FILTER (WHERE estado = 'inspeccion_programada') AS inspecciones_programadas,
        COUNT(*) FILTER (WHERE estado = 'observado') AS observados,
        COUNT(*) FILTER (WHERE estado = 'aprobado') AS aprobados,
        COUNT(*) FILTER (WHERE estado = 'denegado') AS denegados,
        COUNT(*) AS total
      FROM solicitudes
    `);
    const ingresos = await db.query(
      `SELECT COALESCE(SUM(pago_monto),0) AS total FROM solicitudes WHERE pago_confirmado = TRUE`
    );
    res.json({ ...stats.rows[0], ingresos_total: ingresos.rows[0].total });
  } catch (err) {
    res.status(500).json({ error: 'Error al obtener estadísticas' });
  }
});

// Lista de inspectores disponibles
router.get('/municipalidad/inspectores', autenticar, autorizar('municipalidad'), async (req, res) => {
  const db = require('../../config/db');
  try {
    const { rows } = await db.query(
      "SELECT id, email FROM usuarios WHERE rol = 'inspector' AND activo = TRUE ORDER BY email"
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: 'Error al obtener inspectores' });
  }
});


const renovacionCtrl = require('../controllers/renovacionController');
const supervisionCtrl = require('../controllers/supervisionController');

// RENOVACION (negocio)
router.get('/renovaciones/mis-licencias', autenticar, autorizar('negocio'), renovacionCtrl.misLicencias);
router.post('/renovaciones/:licenciaId/renovar', autenticar, autorizar('negocio'), renovacionCtrl.renovarLicencia);

// SUPERVISIONES (inspector)
router.get('/supervisiones/licencias-activas', autenticar, autorizar('inspector'), supervisionCtrl.licenciasActivas);
router.post('/supervisiones/:licenciaId/registrar', autenticar, autorizar('inspector'), supervisionCtrl.registrarSupervision);

// SUPERVISIONES (municipalidad)
router.get('/supervisiones/historial', autenticar, autorizar('municipalidad'), supervisionCtrl.listarSupervisiones);

module.exports = router;
