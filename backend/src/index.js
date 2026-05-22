require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const path = require('path');
const fs = require('fs');

const routes = require('./routes');

const app = express();
app.set("trust proxy", 1);

// ── Directorios de uploads ────────────────────────────────────
['uploads/planos', 'uploads/licencias'].forEach(dir => {
  const full = path.join(__dirname, '..', dir);
  if (!fs.existsSync(full)) fs.mkdirSync(full, { recursive: true });
});

// ── Middlewares de seguridad ──────────────────────────────────
app.use(helmet({
  crossOriginResourcePolicy: { policy: 'cross-origin' }
}));

app.use(cors({
  origin: true,
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

// Rate limiting general
app.use(rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutos
  max: 200,
  message: { error: 'Demasiadas solicitudes, intenta más tarde' }
}));

// Rate limiting estricto para auth
app.use('/api/auth/login', rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: { error: 'Demasiados intentos de login, espera 15 minutos' }
}));

app.use(morgan('combined'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// ── Archivos estáticos (solo uploads propios) ─────────────────
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// ── Rutas API ────────────────────────────────────────────────
app.use('/api', routes);

// ── Health check ─────────────────────────────────────────────
app.get('/health', (req, res) => res.json({ status: 'ok', ts: new Date() }));

// ── Manejo de errores ─────────────────────────────────────────
app.use((err, req, res, next) => {
  console.error(err);
  if (err.code === 'LIMIT_FILE_SIZE')
    return res.status(400).json({ error: 'El archivo supera el límite de 10MB' });
  res.status(500).json({ error: 'Error interno del servidor' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🏛️  Licencias Trujillo API corriendo en puerto ${PORT}`);
  console.log(`   Entorno: ${process.env.NODE_ENV || 'development'}`);
});
