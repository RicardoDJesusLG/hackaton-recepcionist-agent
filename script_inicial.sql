-- 1. Crear extensión para manejar UUIDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Crear ENUM para los estados de las citas
CREATE TYPE estado_cita AS ENUM ('PENDIENTE', 'CONFIRMADA', 'CANCELADA');

-- 3. Tabla de Empresas (Modificada con campos de información del negocio y plan de suscripción)
CREATE TABLE empresas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(100) NOT NULL,
    whatsapp_phone_id VARCHAR(50) UNIQUE NOT NULL,
    whatsapp_token TEXT,
    direccion TEXT,                                 -- ¡Para que la IA sepa dónde están!
    descripcion_negocio TEXT,                       -- Ej: "Barbería premium estilo clásico"
    suscripcion_activa BOOLEAN NOT NULL DEFAULT FALSE,
    plan_suscripcion VARCHAR(50) NOT NULL DEFAULT 'BASIC', -- BASIC, PREMIUM
    telefono_contacto VARCHAR(20),
    maps_link TEXT,
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3b. Tabla de Propietarios (Owners) para login administrativo
CREATE TABLE owners (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    empresa_id UUID NOT NULL REFERENCES empresas(id) ON DELETE CASCADE
);

-- 4. Tabla de Usuarios / Clientes Finales
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(100),
    telefono_whatsapp VARCHAR(20) UNIQUE NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tabla de Servicios por Empresa (Catálogo)
CREATE TABLE servicios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    empresa_id UUID NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    duracion_minutos INT NOT NULL DEFAULT 30,
    precio NUMERIC(10, 2) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    tipo_promocion VARCHAR(50),                      -- NINGUNA, PREDEFINIDA, DESCUENTO_PORCENTAJE, PERSONALIZADA, SERVICIO_GRATIS, DOS_POR_UNO
    valor_promocion VARCHAR(100),
    promocion_activa BOOLEAN,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Tabla de Configuración de Agenda (Horarios de apertura)
CREATE TABLE agenda_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    empresa_id UUID NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    dia_semana INT NOT NULL CHECK (dia_semana BETWEEN 0 AND 6),
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT unique_empresa_dia UNIQUE (empresa_id, dia_semana)
);

-- 7. Tabla de Citas / Reservaciones (Modificada con campos de auditoría)
CREATE TABLE citas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    empresa_id UUID NOT NULL REFERENCES empresas(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    servicio_id UUID NOT NULL REFERENCES servicios(id) ON DELETE RESTRICT,
    fecha_hora_inicio TIMESTAMP NOT NULL,
    fecha_hora_fin TIMESTAMP NOT NULL,
    estado estado_cita DEFAULT 'PENDIENTE',
    detalles_adicionales TEXT,                      -- Ej: "Cancelado por el usuario vía WhatsApp"
    google_event_id VARCHAR(255),                   -- ID del evento sincronizado en Google Calendar
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Para saber cuándo se reprogramó/canceló
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Tabla de Configuración de Google Calendar OAuth (tokens por empresa)
CREATE TABLE google_calendar_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    empresa_id UUID NOT NULL REFERENCES empresas(id) ON DELETE CASCADE UNIQUE,
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    token_type VARCHAR(50),
    scope TEXT,
    calendar_id VARCHAR(255) DEFAULT 'primary',
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Índices óptimos
CREATE INDEX idx_citas_empresa_fecha ON citas(empresa_id, fecha_hora_inicio);
CREATE INDEX idx_usuarios_whatsapp ON usuarios(telefono_whatsapp);
CREATE INDEX idx_servicios_empresa ON servicios(empresa_id);