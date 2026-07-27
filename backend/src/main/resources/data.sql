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

