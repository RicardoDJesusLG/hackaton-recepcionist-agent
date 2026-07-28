-- Migración automática: Renombrar 'username' a 'email' en tabla owners
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'owners' AND column_name = 'username') THEN
        ALTER TABLE owners RENAME COLUMN username TO email;
        ALTER TABLE owners ALTER COLUMN email TYPE varchar(150);
        RAISE NOTICE '[Migration] Columna username renombrada a email exitosamente.';
    END IF;
END
$$;

-- Migración automática: Agregar columnas de campos obligatorios en tabla empresas
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'empresas' AND column_name = 'requiere_nombre') THEN
        ALTER TABLE empresas ADD COLUMN requiere_nombre BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'empresas' AND column_name = 'requiere_telefono') THEN
        ALTER TABLE empresas ADD COLUMN requiere_telefono BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'empresas' AND column_name = 'requiere_correo') THEN
        ALTER TABLE empresas ADD COLUMN requiere_correo BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END
$$;

-- Migración automática: Agregar columna correo en tabla usuarios
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'usuarios' AND column_name = 'correo') THEN
        ALTER TABLE usuarios ADD COLUMN correo VARCHAR(150);
    END IF;
END
$$;

-- Migración automática: Agregar columnas de Menú Multimedia en tabla empresas
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'empresas' AND column_name = 'url_menu_imagen') THEN
        ALTER TABLE empresas ADD COLUMN url_menu_imagen TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'empresas' AND column_name = 'activar_envio_menu') THEN
        ALTER TABLE empresas ADD COLUMN activar_envio_menu BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'empresas' AND column_name = 'envio_menu_inmediato') THEN
        ALTER TABLE empresas ADD COLUMN envio_menu_inmediato BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END
$$;

