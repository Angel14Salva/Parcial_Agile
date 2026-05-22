# Sistema de Licencias de Funcionamiento
## Municipalidad Provincial de Trujillo

---

## 🚀 Setup rápido (desde cero)

### Requisitos
- Node.js 18+
- PostgreSQL 14+
- Cuenta en [Render](https://render.com)
- Cuenta en [Culqi](https://culqi.com) (sandbox)

---

## 1. Base de datos

Crea una base de datos PostgreSQL y ejecuta:
```bash
psql -U tu_usuario -d licencias_trujillo -f backend/config/schema.sql
```

---

## 2. Backend

```bash
cd backend
npm install
cp .env.example .env
# Edita .env con tus credenciales reales
npm run dev
```

El backend corre en `http://localhost:3000`

### Variables de entorno requeridas (.env):
- `DATABASE_URL` — conexión PostgreSQL
- `JWT_SECRET` — secreto largo y aleatorio
- `CULQI_SECRET_KEY` — de tu cuenta Culqi sandbox
- `FRONTEND_URL` — URL del frontend (para CORS)

---

## 3. Frontend

```bash
cd frontend
npm install
cp .env.example .env
# Edita VITE_API_URL si es necesario
npm run dev
```

El frontend corre en `http://localhost:5173`

### Variables de entorno (.env):
- `VITE_API_URL` — URL del backend (ej: https://tu-backend.onrender.com/api)

---

## 4. Escudo de Trujillo (para el PDF)

Copia la imagen del escudo al backend:
```bash
cp escudo-trujillo.png backend/assets/escudo-trujillo.png
```

---

## 5. Deploy en Render

### Backend (Web Service):
- **Build command:** `npm install`
- **Start command:** `npm start`
- **Variables de entorno:** igual que `.env` pero con valores de producción
- **Base de datos:** crea un PostgreSQL en Render y usa la URL interna

### Frontend (Static Site):
- **Build command:** `npm install && npm run build`
- **Publish directory:** `dist`
- **Variable:** `VITE_API_URL=https://tu-backend.onrender.com/api`

---

## 👥 Usuarios de prueba

Crea un inspector desde el panel de municipalidad:
- Municipalidad: `admin@muniTrujillo.gob.pe` (cambiar password al iniciar)
- Inspectores: crear desde Panel Municipalidad → (falta implementar UI)
- Negocios: registro público en `/registro`

---

## 🔐 Seguridad implementada

- JWT con expiración 8h
- bcrypt con salt 12 para contraseñas
- Rate limiting: 200 req/15min general, 10 req/15min en login
- Helmet.js para headers de seguridad
- CORS restrictivo por dominio
- Auditoría completa de acciones
- Validación de RUC con algoritmo oficial peruano
- SSL en PostgreSQL en producción

---

## 📋 Flujo del sistema

```
Negocio → Registro → Validar RUC (SUNAT) → Subir plano → Pagar S/180 (Culqi)
    ↓
Municipalidad → Programar inspección → Asignar inspector
    ↓
Inspector → Registrar resultado
    ├── Conforme → Genera PDF licencia → Negocio descarga
    └── Observado (1ra vez) → 30 días → 2da inspección
              ├── Conforme → Genera licencia
              └── Observado → Licencia denegada
```

---

## 📄 Verificación pública

Cualquier persona puede verificar una licencia en:
`/verificar/{codigo}` — sin necesidad de login
