const jwt = require('jsonwebtoken');
const db = require('../../config/db');

const autenticar = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'Token no proporcionado' });
    }

    const token = authHeader.split(' ')[1];
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    const { rows } = await db.query(
      'SELECT id, email, rol, activo FROM usuarios WHERE id = $1',
      [decoded.id]
    );

    if (!rows[0] || !rows[0].activo) {
      return res.status(401).json({ error: 'Usuario no válido o inactivo' });
    }

    req.usuario = rows[0];
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Sesión expirada, inicia sesión nuevamente' });
    }
    return res.status(401).json({ error: 'Token inválido' });
  }
};

const autorizar = (...roles) => (req, res, next) => {
  if (!roles.includes(req.usuario.rol)) {
    return res.status(403).json({ error: 'No tienes permiso para esta acción' });
  }
  next();
};

module.exports = { autenticar, autorizar };
