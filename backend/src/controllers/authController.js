const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const db = require('../../config/db');

const generarToken = (usuario) =>
  jwt.sign(
    { id: usuario.id, rol: usuario.rol },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRES_IN || '8h' }
  );

// POST /api/auth/login
const login = async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password)
      return res.status(400).json({ error: 'Email y contraseña requeridos' });

    const { rows } = await db.query(
      'SELECT * FROM usuarios WHERE email = $1 AND activo = TRUE',
      [email.toLowerCase().trim()]
    );

    if (!rows[0])
      return res.status(401).json({ error: 'Credenciales incorrectas' });

    const valido = await bcrypt.compare(password, rows[0].password_hash);
    if (!valido)
      return res.status(401).json({ error: 'Credenciales incorrectas' });

    // Auditoría
    await db.query(
      "INSERT INTO auditoria (usuario_id, accion, ip) VALUES ($1, 'LOGIN', $2)",
      [rows[0].id, req.ip]
    );

    const token = generarToken(rows[0]);
    res.json({
      token,
      usuario: { id: rows[0].id, email: rows[0].email, rol: rows[0].rol }
    });
  } catch (err) {
    console.error('login error:', err);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
};

// POST /api/auth/registro (solo negocios se registran solos)
const registro = async (req, res) => {
  try {
    const { email, password, confirmPassword } = req.body;

    if (!email || !password || !confirmPassword)
      return res.status(400).json({ error: 'Todos los campos son requeridos' });

    if (password !== confirmPassword)
      return res.status(400).json({ error: 'Las contraseñas no coinciden' });

    if (password.length < 8)
      return res.status(400).json({ error: 'La contraseña debe tener al menos 8 caracteres' });

    // Verificar email único
    const existe = await db.query('SELECT id FROM usuarios WHERE email = $1', [email.toLowerCase()]);
    if (existe.rows[0])
      return res.status(409).json({ error: 'El email ya está registrado' });

    const hash = await bcrypt.hash(password, 12);

    const { rows } = await db.query(
      'INSERT INTO usuarios (email, password_hash, rol) VALUES ($1, $2, $3) RETURNING id, email, rol',
      [email.toLowerCase().trim(), hash, 'negocio']
    );

    await db.query(
      "INSERT INTO auditoria (usuario_id, accion, ip) VALUES ($1, 'REGISTRO', $2)",
      [rows[0].id, req.ip]
    );

    const token = generarToken(rows[0]);
    res.status(201).json({
      token,
      usuario: rows[0]
    });
  } catch (err) {
    console.error('registro error:', err);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
};

// POST /api/auth/crear-usuario (solo municipalidad crea inspectores)
const crearUsuario = async (req, res) => {
  try {
    const { email, password, rol } = req.body;

    if (!['inspector', 'municipalidad'].includes(rol))
      return res.status(400).json({ error: 'Rol inválido' });

    const existe = await db.query('SELECT id FROM usuarios WHERE email = $1', [email.toLowerCase()]);
    if (existe.rows[0])
      return res.status(409).json({ error: 'El email ya está registrado' });

    const hash = await bcrypt.hash(password, 12);
    const { rows } = await db.query(
      'INSERT INTO usuarios (email, password_hash, rol) VALUES ($1, $2, $3) RETURNING id, email, rol',
      [email.toLowerCase().trim(), hash, rol]
    );

    await db.query(
      "INSERT INTO auditoria (usuario_id, accion, detalle, ip) VALUES ($1, 'CREAR_USUARIO', $2, $3)",
      [req.usuario.id, JSON.stringify({ nuevo_usuario: rows[0].id, rol }), req.ip]
    );

    res.status(201).json({ mensaje: 'Usuario creado exitosamente', usuario: rows[0] });
  } catch (err) {
    console.error('crearUsuario error:', err);
    res.status(500).json({ error: 'Error interno del servidor' });
  }
};

// GET /api/auth/perfil
const perfil = async (req, res) => {
  try {
    const { rows } = await db.query(
      'SELECT id, email, rol, creado_en FROM usuarios WHERE id = $1',
      [req.usuario.id]
    );
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Error interno' });
  }
};

module.exports = { login, registro, crearUsuario, perfil };
