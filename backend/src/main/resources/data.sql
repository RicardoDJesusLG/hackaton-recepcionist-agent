-- ====================================================================
-- Migración automática compatible con Spring Boot Init y PostgreSQL
-- Nota: Usamos sentencias SQL estándar en lugar de bloques procedimentales DO $$ 
-- para evitar errores de parseo por delimitadores de punto y coma (;) en Spring Boot.
-- ====================================================================

-- 1. Asegurar columna email en tabla owners
ALTER TABLE owners ADD COLUMN IF NOT EXISTS email VARCHAR(150);

-- 2. Agregar columnas de campos obligatorios en tabla empresas
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS requiere_nombre BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS requiere_telefono BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS requiere_correo BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Agregar columna correo en tabla usuarios
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS correo VARCHAR(150);

-- 4. Agregar columnas de Menú Multimedia en tabla empresas
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS url_menu_imagen TEXT;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS activar_envio_menu BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS envio_menu_inmediato BOOLEAN NOT NULL DEFAULT FALSE;
