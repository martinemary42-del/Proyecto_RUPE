-- RUPE - Migracion para conservar fotos de avistamientos en MySQL.
-- Ejecutar una sola vez en la base de produccion antes de desplegar el backend actualizado.
-- Las columnas permiten que las fotos se muestren aunque Render reinicie el servicio.

ALTER TABLE avistamiento
  ADD COLUMN foto_contenido LONGBLOB NULL AFTER foto_avistamiento,
  ADD COLUMN foto_tipo_contenido VARCHAR(80) NULL AFTER foto_contenido,
  ADD COLUMN foto_nombre_archivo VARCHAR(180) NULL AFTER foto_tipo_contenido;
