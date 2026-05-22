-- ============================================================
--  Sistema de Licencias de Funcionamiento - Municipalidad de Trujillo
--  Schema PostgreSQL
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── USUARIOS ───────────────────────────────────────────────
CREATE TABLE usuarios (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email         VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  rol           VARCHAR(20) NOT NULL CHECK (rol IN ('negocio','inspector','municipalidad')),
  activo        BOOLEAN DEFAULT TRUE,
  creado_en     TIMESTAMPTZ DEFAULT NOW(),
  actualizado_en TIMESTAMPTZ DEFAULT NOW()
);

-- ─── NEGOCIOS ────────────────────────────────────────────────
CREATE TABLE negocios (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  usuario_id        UUID NOT NULL REFERENCES usuarios(id),
  ruc               VARCHAR(11) UNIQUE NOT NULL,
  razon_social      VARCHAR(300) NOT NULL,
  nombre_comercial  VARCHAR(300),
  domicilio_fiscal  TEXT NOT NULL,
  rubro             VARCHAR(200) NOT NULL,
  area_m2           DECIMAL(10,2),
  -- Representante legal
  rep_nombre        VARCHAR(200) NOT NULL,
  rep_dni           VARCHAR(8) NOT NULL,
  rep_telefono      VARCHAR(15),
  -- Validación SUNAT
  sunat_validado    BOOLEAN DEFAULT FALSE,
  sunat_validado_en TIMESTAMPTZ,
  creado_en         TIMESTAMPTZ DEFAULT NOW()
);

-- ─── SOLICITUDES ─────────────────────────────────────────────
CREATE TABLE solicitudes (
  id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  negocio_id          UUID NOT NULL REFERENCES negocios(id),
  numero_expediente   VARCHAR(20) UNIQUE NOT NULL,
  estado              VARCHAR(30) NOT NULL DEFAULT 'pendiente_pago'
                      CHECK (estado IN (
                        'pendiente_pago',
                        'documentos_pendientes',
                        'en_validacion',
                        'pendiente_inspeccion',
                        'inspeccion_programada',
                        'observado',
                        'segunda_inspeccion_programada',
                        'aprobado',
                        'denegado',
                        'renovacion_pendiente'
                      )),
  -- Pago
  pago_monto          DECIMAL(8,2) DEFAULT 180.00,
  pago_culqi_id       VARCHAR(200),
  pago_fecha          TIMESTAMPTZ,
  pago_confirmado     BOOLEAN DEFAULT FALSE,
  -- Archivos
  plano_url           TEXT,
  plano_actualizado   BOOLEAN DEFAULT FALSE,
  -- Fechas
  fecha_solicitud     TIMESTAMPTZ DEFAULT NOW(),
  fecha_vencimiento   TIMESTAMPTZ,
  creado_en           TIMESTAMPTZ DEFAULT NOW(),
  actualizado_en      TIMESTAMPTZ DEFAULT NOW()
);

-- ─── INSPECCIONES ────────────────────────────────────────────
CREATE TABLE inspecciones (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  solicitud_id      UUID NOT NULL REFERENCES solicitudes(id),
  inspector_id      UUID REFERENCES usuarios(id),
  numero_inspeccion INTEGER NOT NULL CHECK (numero_inspeccion IN (1,2)),
  fecha_programada  DATE NOT NULL,
  hora_programada   TIME,
  fecha_realizada   TIMESTAMPTZ,
  resultado         VARCHAR(20) CHECK (resultado IN ('conforme','observado','pendiente')),
  observaciones     TEXT,
  -- Si el negocio debe subir docs corregidos
  requiere_docs     BOOLEAN DEFAULT FALSE,
  docs_corregidos   BOOLEAN DEFAULT FALSE,
  creado_en         TIMESTAMPTZ DEFAULT NOW()
);

-- ─── LICENCIAS ───────────────────────────────────────────────
CREATE TABLE licencias (
  id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  solicitud_id      UUID NOT NULL REFERENCES solicitudes(id),
  negocio_id        UUID NOT NULL REFERENCES negocios(id),
  numero_licencia   VARCHAR(20) UNIQUE NOT NULL,
  numero_resolucion VARCHAR(50) NOT NULL,
  fecha_emision     DATE NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  pdf_url           TEXT,
  activa            BOOLEAN DEFAULT TRUE,
  codigo_verificacion VARCHAR(20) UNIQUE NOT NULL,
  creado_en         TIMESTAMPTZ DEFAULT NOW()
);

-- ─── SUPERVISIONES (inspecciones sorpresa) ───────────────────
CREATE TABLE supervisiones (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  licencia_id     UUID NOT NULL REFERENCES licencias(id),
  inspector_id    UUID NOT NULL REFERENCES usuarios(id),
  fecha           DATE NOT NULL,
  resultado       VARCHAR(20) CHECK (resultado IN ('conforme','infraccion')),
  observaciones   TEXT,
  multa_aplicada  BOOLEAN DEFAULT FALSE,
  multa_monto     DECIMAL(10,2),
  licencia_revocada BOOLEAN DEFAULT FALSE,
  creado_en       TIMESTAMPTZ DEFAULT NOW()
);

-- ─── AUDITORIA ───────────────────────────────────────────────
CREATE TABLE auditoria (
  id          BIGSERIAL PRIMARY KEY,
  usuario_id  UUID REFERENCES usuarios(id),
  accion      VARCHAR(100) NOT NULL,
  tabla       VARCHAR(50),
  registro_id UUID,
  detalle     JSONB,
  ip          INET,
  creado_en   TIMESTAMPTZ DEFAULT NOW()
);

-- ─── ÍNDICES ─────────────────────────────────────────────────
CREATE INDEX idx_solicitudes_negocio ON solicitudes(negocio_id);
CREATE INDEX idx_solicitudes_estado  ON solicitudes(estado);
CREATE INDEX idx_inspecciones_sol    ON inspecciones(solicitud_id);
CREATE INDEX idx_inspecciones_insp   ON inspecciones(inspector_id);
CREATE INDEX idx_licencias_negocio   ON licencias(negocio_id);
CREATE INDEX idx_licencias_codigo    ON licencias(codigo_verificacion);
CREATE INDEX idx_auditoria_usuario   ON auditoria(usuario_id);
CREATE INDEX idx_auditoria_fecha     ON auditoria(creado_en);

-- ─── SECUENCIAS para numeración ──────────────────────────────
CREATE SEQUENCE seq_expediente START 1000;
CREATE SEQUENCE seq_licencia   START 1000;
CREATE SEQUENCE seq_resolucion START 2500;

-- ─── FUNCIÓN para generar número de expediente ───────────────
CREATE OR REPLACE FUNCTION generar_expediente()
RETURNS TEXT AS $$
BEGIN
  RETURN 'I' || TO_CHAR(NOW(), 'YYYY') || LPAD(nextval('seq_expediente')::TEXT, 7, '0');
END;
$$ LANGUAGE plpgsql;

-- ─── FUNCIÓN para generar número de licencia ─────────────────
CREATE OR REPLACE FUNCTION generar_numero_licencia()
RETURNS TEXT AS $$
BEGIN
  RETURN LPAD(nextval('seq_licencia')::TEXT, 7, '0');
END;
$$ LANGUAGE plpgsql;

-- ─── TRIGGER updated_at ──────────────────────────────────────
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.actualizado_en = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_solicitudes_updated
  BEFORE UPDATE ON solicitudes
  FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_usuarios_updated
  BEFORE UPDATE ON usuarios
  FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- ─── USUARIO MUNICIPALIDAD por defecto ───────────────────────
-- Cambiar password en producción
INSERT INTO usuarios (email, password_hash, rol)
VALUES (
  'admin@muniTrujillo.gob.pe',
  '$2a$12$placeholder_change_on_first_run',
  'municipalidad'
);
