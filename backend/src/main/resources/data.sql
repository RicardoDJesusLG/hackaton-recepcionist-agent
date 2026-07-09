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
