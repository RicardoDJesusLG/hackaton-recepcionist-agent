-- Migración: Renombrar columna 'username' a 'email' en la tabla owners
-- Ejecutar este script manualmente si Hibernate no puede manejar el cambio automáticamente
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'owners' AND column_name = 'username') THEN
        ALTER TABLE owners RENAME COLUMN username TO email;
        ALTER TABLE owners ALTER COLUMN email TYPE varchar(150);
        RAISE NOTICE 'Columna renombrada de username a email exitosamente.';
    ELSE
        RAISE NOTICE 'La columna username no existe o ya fue renombrada.';
    END IF;
END
$$;
