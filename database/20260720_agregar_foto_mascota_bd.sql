-- RUPE - Migracion para conservar fotos principales de mascotas en MySQL.
-- Ejecutar una sola vez en la base de produccion antes de desplegar el backend actualizado.
-- Las columnas permiten que las fotos de mascotas se muestren aunque Render reinicie el servicio.

ALTER TABLE fotografia
  ADD COLUMN contenido LONGBLOB NULL AFTER tipo_fotografia,
  ADD COLUMN tipo_contenido VARCHAR(80) NULL AFTER contenido;
