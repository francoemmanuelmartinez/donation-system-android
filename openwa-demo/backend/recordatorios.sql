-- openwa-demo/backend/recordatorios.sql
-- Importá esto en phpMyAdmin (o por consola) ANTES de usar los archivos PHP.
--
--   mysql -u root openwa_demo < recordatorios.sql
--
-- OJO: "openwa_demo" es solo un nombre de EJEMPLO. Usá el nombre de tu propia
-- base de datos y poné ese mismo nombre en DB_NAME (openwa_config.php).
--
-- Esta es la tabla de EJEMPLO de la guía. Si ya tenés tu propia tabla de
-- recordatorios, no corras esto: ajustá los archivos PHP (buscá los "TODO").

CREATE TABLE recordatorios (
  id INT AUTO_INCREMENT PRIMARY KEY,
  destinatario_nombre VARCHAR(120) NOT NULL,
  telefono VARCHAR(30) NOT NULL,
  mensaje VARCHAR(500) NOT NULL,
  fecha_envio DATE NOT NULL,
  programado_para DATETIME NULL,
  enviado TINYINT(1) NOT NULL DEFAULT 0,
  enviado_en DATETIME NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Un par de filas de prueba.
-- IMPORTANTE: cambia el telefono por el TUYO si querés recibir el mensaje de verdad.
INSERT INTO recordatorios (destinatario_nombre, telefono, mensaje, fecha_envio) VALUES
('Juan Pérez', '5491100000000', 'Hola Juan, te esperamos mañana para tu turno.', CURDATE()),
('María López', '5491111111111', 'Hola María, recordá que mañana tenés turno.', CURDATE());



