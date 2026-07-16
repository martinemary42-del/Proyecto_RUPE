-- RUPE - Script normalizado de creacion de base de datos
-- Proyecto Terminal II
-- Fuente: Elaboracion propia, 2026.
-- Objetivo: separar entidades transaccionales y catalogos mediante llaves foraneas.
-- Base limpia para despliegue y revision: bd_rupe_produccion.
-- Importante: este script crea una base nueva y no toca respaldos locales.

CREATE DATABASE IF NOT EXISTS bd_rupe_produccion
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bd_rupe_produccion;

SET FOREIGN_KEY_CHECKS = 0;

-- Catalogo de roles del sistema. Define permisos generales como administrador, dueno o ciudadano.
CREATE TABLE catalogo_rol (
  id_rol INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL UNIQUE,
  descripcion VARCHAR(150),
  activo BOOLEAN NOT NULL DEFAULT TRUE /* Borrado logico: permite desactivar sin perder historial */
) ENGINE=InnoDB;

-- Catalogo para clasificar el tipo de mascota. En esta etapa se usa principalmente perro con posible incremento a otras mascotas
CREATE TABLE catalogo_tipo_mascota (
  id_tipo_mascota INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL UNIQUE,
  activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Catalogo de razas. Depende del tipo de mascota para evitar razas fuera de contexto.
CREATE TABLE catalogo_raza (
  id_raza INT AUTO_INCREMENT PRIMARY KEY,
  id_tipo_mascota INT NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_raza_tipo_nombre UNIQUE (id_tipo_mascota, nombre),
  CONSTRAINT fk_raza_tipo FOREIGN KEY (id_tipo_mascota)
    REFERENCES catalogo_tipo_mascota(id_tipo_mascota)
) ENGINE=InnoDB;

-- Catalogo de colores base para describir a la mascota de forma consistente.
CREATE TABLE catalogo_color (
  id_color INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL UNIQUE,
  activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Catalogo de estados considerados dentro de la cobertura del sistema.
CREATE TABLE catalogo_estado (
  id_estado INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE,
  activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Catalogo de municipios o alcaldias. Cada municipio pertenece a un estado.
CREATE TABLE catalogo_municipio (
  id_municipio INT AUTO_INCREMENT PRIMARY KEY,
  id_estado INT NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  zona_cobertura VARCHAR(80) NOT NULL DEFAULT 'Cobertura inicial',
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_municipio_estado_nombre UNIQUE (id_estado, nombre),
  CONSTRAINT fk_municipio_estado FOREIGN KEY (id_estado)
    REFERENCES catalogo_estado(id_estado)
) ENGINE=InnoDB;

-- Catalogo de colonias. Cada colonia pertenece a un municipio y concentra el codigo postal.
CREATE TABLE catalogo_colonia (
  id_colonia INT AUTO_INCREMENT PRIMARY KEY,
  id_municipio INT NOT NULL,
  nombre VARCHAR(150) NOT NULL,
  codigo_postal VARCHAR(10),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_colonia_municipio_nombre UNIQUE (id_municipio, nombre),
  CONSTRAINT fk_colonia_municipio FOREIGN KEY (id_municipio)
    REFERENCES catalogo_municipio(id_municipio)
) ENGINE=InnoDB;

-- Catalogo de estatus usados para reportes, avistamientos y seguimiento.
CREATE TABLE catalogo_estatus (
  id_estatus INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL UNIQUE,
  descripcion VARCHAR(150),
  activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Tabla de usuarios registrados. Guarda datos de acceso, rol, contacto y estado de la cuenta.
CREATE TABLE usuario (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  id_rol INT NOT NULL,
  nombre_completo VARCHAR(120) NOT NULL,
  correo VARCHAR(120) NOT NULL UNIQUE,
  telefono VARCHAR(20),
  password_hash VARCHAR(120) NOT NULL,
  intentos_fallidos INT NOT NULL DEFAULT 0,
  fecha_bloqueo DATETIME,
  ultimo_acceso DATETIME,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP /* Fecha en que se crea el registro */,
  fecha_actualizacion DATETIME,
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
    REFERENCES catalogo_rol(id_rol)
) ENGINE=InnoDB;

-- Tokens temporales para recuperar contrasena. Se crean solo cuando un usuario solicita recuperacion.
CREATE TABLE password_reset_token (
  id_token INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL,
  token VARCHAR(80) NOT NULL UNIQUE,
  fecha_expiracion DATETIME NOT NULL,
  usado BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_password_reset_usuario FOREIGN KEY (id_usuario)
    REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Tabla principal de mascotas registradas. Usa catalogos para tipo, raza, color y ubicacion de referencia.
CREATE TABLE mascota (
  id_mascota INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NOT NULL,
  id_tipo_mascota INT NOT NULL,
  id_raza INT NOT NULL,
  id_color INT NOT NULL,
  id_colonia_referencia INT NOT NULL,
  nombre VARCHAR(80) NOT NULL,
  sexo VARCHAR(40) NOT NULL,
  edad_aproximada VARCHAR(60),
  mezcla_raza VARCHAR(120),
  descripcion_color VARCHAR(180),
  senas_particulares TEXT NOT NULL,
  condicion_medica TEXT,
  collar_placa VARCHAR(40) NOT NULL,
  calle VARCHAR(120),
  numero VARCHAR(20),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion DATETIME,
  fecha_vencimiento DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL 30 DAY),
  fecha_ultima_renovacion DATETIME,
  renovaciones INT NOT NULL DEFAULT 0,
  requiere_renovacion BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_mascota_usuario FOREIGN KEY (id_usuario)
    REFERENCES usuario(id_usuario),
  CONSTRAINT fk_mascota_tipo FOREIGN KEY (id_tipo_mascota)
    REFERENCES catalogo_tipo_mascota(id_tipo_mascota),
  CONSTRAINT fk_mascota_raza FOREIGN KEY (id_raza)
    REFERENCES catalogo_raza(id_raza),
  CONSTRAINT fk_mascota_color FOREIGN KEY (id_color)
    REFERENCES catalogo_color(id_color),
  CONSTRAINT fk_mascota_colonia_ref FOREIGN KEY (id_colonia_referencia)
    REFERENCES catalogo_colonia(id_colonia)
) ENGINE=InnoDB;

-- Fotografias asociadas a una mascota. Se guarda la ruta del archivo, no la imagen como dato binario.
CREATE TABLE fotografia (
  id_fotografia INT AUTO_INCREMENT PRIMARY KEY,
  id_mascota INT NOT NULL,
  ruta_archivo VARCHAR(250) NOT NULL,
  nombre_archivo VARCHAR(180) NOT NULL,
  tipo_fotografia VARCHAR(40) NOT NULL DEFAULT 'PRINCIPAL',
  es_principal BOOLEAN NOT NULL DEFAULT TRUE,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_fotografia_mascota FOREIGN KEY (id_mascota)
    REFERENCES mascota(id_mascota)
) ENGINE=InnoDB;

-- Reportes de extravio o robo. Se relacionan con una mascota y con catalogos de ubicacion.
CREATE TABLE reporte_extravio (
  id_reporte INT AUTO_INCREMENT PRIMARY KEY,
  id_mascota INT NOT NULL,
  id_estatus INT NOT NULL,
  id_colonia INT NOT NULL,
  folio VARCHAR(40) NOT NULL UNIQUE,
  qr_url VARCHAR(250),
  fecha_extravio DATE NOT NULL,
  tipo_reporte VARCHAR(20) NOT NULL DEFAULT 'EXTRAVIO',
  calle VARCHAR(120),
  numero VARCHAR(20),
  referencias TEXT,
  descripcion_hechos TEXT NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion DATETIME,
  fecha_vencimiento DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL 30 DAY),
  fecha_ultima_renovacion DATETIME,
  renovaciones INT NOT NULL DEFAULT 0,
  requiere_renovacion BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_reporte_mascota FOREIGN KEY (id_mascota)
    REFERENCES mascota(id_mascota),
  CONSTRAINT fk_reporte_estatus FOREIGN KEY (id_estatus)
    REFERENCES catalogo_estatus(id_estatus),
  CONSTRAINT fk_reporte_colonia FOREIGN KEY (id_colonia)
    REFERENCES catalogo_colonia(id_colonia)
) ENGINE=InnoDB;

-- Avistamientos ciudadanos. Permiten registrar donde fue vista o resguardada una mascota.
CREATE TABLE avistamiento (
  id_avistamiento INT AUTO_INCREMENT PRIMARY KEY,
  id_reporte INT,
  id_estatus INT NOT NULL,
  id_colonia INT NOT NULL,
  folio_avistamiento VARCHAR(20),
  fecha_avistamiento DATE NOT NULL,
  calle VARCHAR(120),
  numero VARCHAR(20),
  referencias TEXT,
  descripcion TEXT NOT NULL,
  foto_avistamiento VARCHAR(250),
  resguardado BOOLEAN NOT NULL DEFAULT FALSE,
  nombre_resguardante VARCHAR(120),
  correo_resguardante VARCHAR(120),
  telefono_resguardante VARCHAR(20),
  validado_dueno BOOLEAN NOT NULL DEFAULT FALSE,
  fecha_validacion DATETIME,
  comentario_validacion VARCHAR(500),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_avistamiento_reporte FOREIGN KEY (id_reporte)
    REFERENCES reporte_extravio(id_reporte),
  CONSTRAINT fk_avistamiento_estatus FOREIGN KEY (id_estatus)
    REFERENCES catalogo_estatus(id_estatus),
  CONSTRAINT fk_avistamiento_colonia FOREIGN KEY (id_colonia)
    REFERENCES catalogo_colonia(id_colonia)
) ENGINE=InnoDB;

-- Notificaciones generadas por el sistema, por ejemplo nuevos avistamientos o cierres de caso.
CREATE TABLE notificacion (
  id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
  id_reporte INT NOT NULL,
  tipo VARCHAR(50) NOT NULL,
  destinatario VARCHAR(120) NOT NULL,
  asunto VARCHAR(150) NOT NULL,
  mensaje TEXT NOT NULL,
  fecha_envio DATETIME,
  estatus_envio VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE',
  CONSTRAINT fk_notificacion_reporte FOREIGN KEY (id_reporte)
    REFERENCES reporte_extravio(id_reporte)
) ENGINE=InnoDB;

-- Mensajes enviados desde contacto o soporte. Pueden relacionarse con un reporte especifico.
CREATE TABLE mensaje_contacto (
  id_mensaje_contacto INT AUTO_INCREMENT PRIMARY KEY,
  id_reporte INT,
  nombre VARCHAR(100) NOT NULL,
  correo VARCHAR(120) NOT NULL,
  telefono VARCHAR(20),
  asunto VARCHAR(60) NOT NULL,
  mensaje VARCHAR(500) NOT NULL,
  estatus VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  ip VARCHAR(80),
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_atencion DATETIME,
  respuesta_admin VARCHAR(500),
  CONSTRAINT fk_mensaje_contacto_reporte FOREIGN KEY (id_reporte)
    REFERENCES reporte_extravio(id_reporte)
) ENGINE=InnoDB;

-- Bitacora de seguridad. Registra acciones importantes para auditoria y trazabilidad.
CREATE TABLE bitacora_seguridad (
  id_bitacora_seguridad INT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT,
  correo VARCHAR(120),
  accion VARCHAR(80) NOT NULL,
  modulo VARCHAR(60) NOT NULL,
  resultado VARCHAR(30) NOT NULL,
  ip VARCHAR(80),
  descripcion VARCHAR(500),
  fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_bitacora_seguridad_usuario FOREIGN KEY (id_usuario)
    REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Conteo de visitas por pagina publica para estadisticas administrativas.
CREATE TABLE visita_sitio (
  id_visita INT AUTO_INCREMENT PRIMARY KEY,
  pagina VARCHAR(120) NOT NULL UNIQUE,
  total BIGINT NOT NULL DEFAULT 0,
  fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

-- Datos iniciales de roles. Permiten iniciar el sistema con permisos basicos.
INSERT INTO catalogo_rol (nombre, descripcion, activo) VALUES
('ADMINISTRADOR', 'Gestiona usuarios, reportes, catalogos y estadisticas', TRUE),
('DUENO', 'Usuario que registra mascotas y reportes de extravio', TRUE),
('CIUDADANO', 'Rol historico inactivo; los avistamientos publicos no requieren cuenta', FALSE);

-- Tipo de mascota inicial usado por RUPE.
INSERT INTO catalogo_tipo_mascota (nombre, activo) VALUES
('Perro', TRUE);

-- Razas iniciales para alimentar los formularios y evitar captura libre.
INSERT INTO catalogo_raza (id_tipo_mascota, nombre, activo)
SELECT id_tipo_mascota, 'Criollo/Mestizo', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Labrador', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Golden Retriever', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Chihuahua', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Poodle', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Schnauzer', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Pastor Aleman', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Pitbull', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Husky', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro'
UNION ALL SELECT id_tipo_mascota, 'Otro', TRUE FROM catalogo_tipo_mascota WHERE nombre = 'Perro';

-- Colores iniciales para descripcion uniforme de mascotas.
INSERT INTO catalogo_color (nombre, activo) VALUES
('Negro', TRUE), ('Blanco', TRUE), ('Cafe', TRUE), ('Gris', TRUE),
('Dorado', TRUE), ('Pinto/Manchado', TRUE), ('Atigrado', TRUE), ('Otro', TRUE);

-- Estados iniciales de cobertura.
INSERT INTO catalogo_estado (nombre, activo) VALUES
('Ciudad de Mexico', TRUE),
('Estado de Mexico', TRUE);

-- Municipios y alcaldias iniciales de cobertura.
INSERT INTO catalogo_municipio (id_estado, nombre, zona_cobertura, activo)
SELECT e.id_estado, m.nombre, m.zona, TRUE
FROM catalogo_estado e
JOIN (
  SELECT 'Ciudad de Mexico' estado, 'Alvaro Obregon' nombre, 'CDMX completa' zona UNION ALL
  SELECT 'Ciudad de Mexico', 'Azcapotzalco', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Benito Juarez', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Coyoacan', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Cuajimalpa de Morelos', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Cuauhtemoc', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Gustavo A. Madero', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Iztacalco', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Iztapalapa', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'La Magdalena Contreras', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Miguel Hidalgo', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Milpa Alta', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Tlahuac', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Tlalpan', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Venustiano Carranza', 'CDMX completa' UNION ALL
  SELECT 'Ciudad de Mexico', 'Xochimilco', 'CDMX completa' UNION ALL
  SELECT 'Estado de Mexico', 'Naucalpan', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Tlalnepantla', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Ecatepec', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Nezahualcoyotl', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Chimalhuacan', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'La Paz', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Ixtapaluca', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Valle de Chalco', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Atizapan de Zaragoza', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Coacalco', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Tultitlan', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Cuautitlan Izcalli', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Huixquilucan', 'Zona conurbada' UNION ALL
  SELECT 'Estado de Mexico', 'Tecamac', 'Zona conurbada'
) m ON e.nombre = m.estado;

-- Colonias iniciales para pruebas y seleccion controlada en formularios.
INSERT INTO catalogo_colonia (id_municipio, nombre, codigo_postal, activo)
SELECT m.id_municipio, c.nombre, c.cp, TRUE
FROM catalogo_municipio m
JOIN catalogo_estado e ON e.id_estado = m.id_estado
JOIN (
  SELECT 'Ciudad de Mexico' estado, 'Coyoacan' municipio, 'Del Carmen' nombre, '04100' cp UNION ALL
  SELECT 'Ciudad de Mexico', 'Coyoacan', 'Copilco Universidad', '04360' UNION ALL
  SELECT 'Ciudad de Mexico', 'Coyoacan', 'Pedregal de Santo Domingo', '04369' UNION ALL
  SELECT 'Ciudad de Mexico', 'Benito Juarez', 'Narvarte', '03020' UNION ALL
  SELECT 'Ciudad de Mexico', 'Benito Juarez', 'Del Valle Centro', '03100' UNION ALL
  SELECT 'Ciudad de Mexico', 'Benito Juarez', 'Portales Norte', '03303' UNION ALL
  SELECT 'Ciudad de Mexico', 'Iztapalapa', 'Santa Cruz Meyehualco', '09290' UNION ALL
  SELECT 'Ciudad de Mexico', 'Iztapalapa', 'San Lorenzo Tezonco', '09790' UNION ALL
  SELECT 'Ciudad de Mexico', 'Iztapalapa', 'Lomas Estrella', '09890' UNION ALL
  SELECT 'Ciudad de Mexico', 'Tlalpan', 'Centro de Tlalpan', '14000' UNION ALL
  SELECT 'Ciudad de Mexico', 'Tlalpan', 'San Miguel Topilejo', '14500' UNION ALL
  SELECT 'Ciudad de Mexico', 'Tlalpan', 'Pedregal de San Nicolas', '14100' UNION ALL
  SELECT 'Estado de Mexico', 'Naucalpan', 'Ciudad Satelite', '53100' UNION ALL
  SELECT 'Estado de Mexico', 'Naucalpan', 'Echegaray', '53300' UNION ALL
  SELECT 'Estado de Mexico', 'Naucalpan', 'Lomas Verdes', '53120' UNION ALL
  SELECT 'Estado de Mexico', 'Ecatepec', 'San Cristobal Centro', '55000' UNION ALL
  SELECT 'Estado de Mexico', 'Ecatepec', 'Ciudad Azteca', '55120' UNION ALL
  SELECT 'Estado de Mexico', 'Ecatepec', 'Jardines de Morelos', '55070' UNION ALL
  SELECT 'Estado de Mexico', 'Nezahualcoyotl', 'Benito Juarez', '57000' UNION ALL
  SELECT 'Estado de Mexico', 'Nezahualcoyotl', 'Las Aguilas', '57900' UNION ALL
  SELECT 'Estado de Mexico', 'Nezahualcoyotl', 'Impulsora', '57130'
) c ON c.estado = e.nombre AND c.municipio = m.nombre;

-- Estatus iniciales para controlar el ciclo de vida de reportes y avistamientos.
INSERT INTO catalogo_estatus (nombre, descripcion, activo) VALUES
('ACTIVO', 'Reporte o registro activo', TRUE),
('PENDIENTE', 'Pendiente de validacion', TRUE),
('VALIDADO', 'Validado por el responsable', TRUE),
('RECUPERADO', 'Mascota recuperada', TRUE),
('DESCARTADO', 'Registro descartado o no procedente', TRUE),
('CERRADO', 'Caso cerrado', TRUE);

-- Usuarios minimos para revision academica.
-- Contrasenas:
-- admin@rupe.local / Admin123!
-- prueba@rupe.com / Prueba123
INSERT INTO usuario (
  id_rol, nombre_completo, correo, telefono, password_hash,
  intentos_fallidos, fecha_bloqueo, ultimo_acceso, activo, fecha_registro, fecha_actualizacion
) VALUES
(
  (SELECT id_rol FROM catalogo_rol WHERE nombre = 'ADMINISTRADOR'),
  'Administrador RUPE',
  'admin@rupe.local',
  '5500000000',
  '$2a$10$r4ag00J0QZ7PZq6nFLgPWuBK4jO5F4NK5.m22c7lb01vNdguBdp7u',
  0, NULL, NULL, TRUE, NOW(), NOW()
),
(
  (SELECT id_rol FROM catalogo_rol WHERE nombre = 'DUENO'),
  'Usuario de Prueba RUPE',
  'prueba@rupe.com',
  '5511111111',
  '$2a$10$05Yiwjn.5LCGR5tvV54BYuAamTXATmZBKqBghZag3HXaZeJHUjl6S',
  0, NULL, NULL, TRUE, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
  nombre_completo = VALUES(nombre_completo),
  telefono = VALUES(telefono),
  password_hash = VALUES(password_hash),
  intentos_fallidos = 0,
  fecha_bloqueo = NULL,
  ultimo_acceso = NULL,
  activo = TRUE,
  fecha_actualizacion = NOW();

