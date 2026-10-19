-- ============================================================
-- SEED: 40 donantes + turnos en la Agenda (octubre 2026)
-- ============================================================
-- Generado bajo pedido: 10 turnos en la semana entrante (mar 13
-- a vie 16/10, omitiendo el feriado del lun 12/10) y 30 turnos
-- repartidos en el resto de octubre, cubriendo TODOS los días
-- hábiles del mes (incluye fechas ya pasadas).
--
-- Incluye además: teléfonos/emails, donaciones previas (con
-- last_donation_date y next_eligible_date calculado según la
-- política de recuperación y día hábil), y 5 restricciones médicas
-- para generar estados mixtos (EN VEDA) en la Agenda.
--
-- IMPORTANTE:
--   * NO borra datos existentes (van donantes nuevos ids 3..42).
--   * Ejecutar UNA sola vez (los DNI son UNIQUE).
--   * Datos 100% ficticios para pruebas/demo.
-- ============================================================

SET NAMES utf8mb4;

-- Diccionario de restricciones (no-op si ya existe)
INSERT IGNORE INTO `restrictions` (`name`, `default_veda_days`) VALUES
('Tatuaje Reciente', 180),
('Cirugía Reciente', 365),
('Bloqueo por Medicación', 30),
('Viaje a Zona Endémica', 90);

-- ------------------------------------------------------------
-- Tabla temporal maestra: datos de los 40 donantes
-- (idx = orden; don_date = última donación histórica;
--  appt_date/appt_time = turno en la Agenda; proc = procedimiento)
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS `tmp_seed`;
CREATE TEMPORARY TABLE `tmp_seed` (
  `idx` INT,
  `dni` VARCHAR(20),
  `first_name` VARCHAR(100),
  `last_name` VARCHAR(100),
  `gender` VARCHAR(20),
  `birth_date` DATE,
  `blood_type` VARCHAR(10),
  `proc` VARCHAR(20),
  `don_date` DATE,
  `appt_date` DATE,
  `appt_time` TIME
);

INSERT INTO `tmp_seed` (`idx`, `dni`, `first_name`, `last_name`, `gender`, `birth_date`, `blood_type`, `proc`, `don_date`, `appt_date`, `appt_time`) VALUES
-- ===== Turnos de "esta semana" (mar 13 → vie 16/10, sin feriado) =====
(1,  '30111222', 'Carlos',    'Fernández', 'Masculino', '2026-04-12', 'O+', 'BLOOD',     '2026-09-15', '2026-10-13', '09:00:00'),
(2,  '30111223', 'María',     'Sosa',      'Femenino',  '2026-08-22', 'A+', 'PLASMA',    '2026-10-02', '2026-10-13', '10:00:00'),
(3,  '30111224', 'Lucía',     'García',    'Femenino',  '2026-11-05', 'B+', 'PLATELETS', '2026-10-01', '2026-10-13', '11:00:00'),
(4,  '30111225', 'Ana',       'Martínez',  'Femenino',  '2026-02-17', 'AB+', 'BLOOD',    '2026-09-14', '2026-10-14', '09:00:00'),
(5,  '30111226', 'Pedro',     'González',  'Masculino', '2026-07-30', 'O-', 'PLASMA',    '2026-10-09', '2026-10-14', '10:00:00'),
(6,  '30111227', 'Sofía',     'Rodríguez', 'Femenino',  '2026-05-09', 'A-', 'PLATELETS', '2026-10-03', '2026-10-15', '09:00:00'),
(7,  '30111228', 'Juan',      'López',     'Masculino', '2026-12-01', 'B-', 'BLOOD',     '2026-08-20', '2026-10-15', '10:00:00'),
(8,  '30111229', 'Valeria',   'Pérez',     'Femenino',  '2026-09-18', 'AB-', 'PLASMA',   '2026-10-01', '2026-10-15', '14:00:00'),
(9,  '30111230', 'Martín',    'Romero',    'Masculino', '2026-04-25', 'O+', 'PLATELETS', '2026-09-30', '2026-10-16', '09:00:00'),
(10, '30111231', 'Camila',    'Sánchez',   'Femenino',  '2026-01-28', 'A+', 'BLOOD',     '2026-08-10', '2026-10-16', '10:00:00'),
-- ===== 30 turnos del resto de octubre (todo el mes, omite feriado 12/10) =====
(11, '30111232', 'Celeste',   'Álvarez',   'Femenino',  '2026-10-11', 'B+', 'PLASMA',    '2026-10-03', '2026-10-01', '09:00:00'),
(12, '30111233', 'Carla',     'Castro',    'Femenino',  '2026-06-03', 'AB+', 'PLATELETS','2026-10-02', '2026-10-01', '10:00:00'),
(13, '30111234', 'Julieta',   'Díaz',      'Femenino',  '2026-07-19', 'O-', 'BLOOD',     '2026-07-11', '2026-10-02', '09:00:00'),
(14, '30111235', 'Diego',     'Morales',   'Masculino', '2026-03-08', 'A-', 'PLASMA',    '2026-09-30', '2026-10-02', '10:00:00'),
(15, '30111236', 'Florencia', 'Rojas',     'Femenino',  '2026-12-24', 'B-', 'PLATELETS','2026-09-29', '2026-10-05', '09:00:00'),
(16, '30111237', 'Marcelo',   'Herrera',   'Masculino', '2026-08-05', 'AB-', 'BLOOD',    '2026-08-08', '2026-10-06', '09:00:00'),
(17, '30111238', 'Paula',     'Molina',    'Femenino',  '2026-04-29', 'O+', 'PLASMA',    '2026-09-29', '2026-10-06', '10:00:00'),
(18, '30111239', 'Romina',    'Vega',      'Femenino',  '2026-10-15', 'A+', 'PLATELETS','2026-09-28', '2026-10-07', '09:00:00'),
(19, '30111240', 'Hernán',    'Medina',    'Masculino', '2026-01-02', 'B+', 'BLOOD',     '2026-08-05', '2026-10-07', '10:00:00'),
(20, '30111241', 'Santiago',  'Acosta',    'Masculino', '2026-06-20', 'AB+', 'PLASMA',   '2026-10-05', '2026-10-08', '09:00:00'),
(21, '30111242', 'Gastón',    'Olivares',  'Masculino', '2026-02-11', 'O-', 'PLATELETS','2026-09-27', '2026-10-08', '10:00:00'),
(22, '30111243', 'Brenda',    'Cabrera',   'Femenino',  '2026-09-27', 'A-', 'BLOOD',     '2026-08-04', '2026-10-09', '09:00:00'),
(23, '30111244', 'Naiara',    'Ferreyra',  'Femenino',  '2026-03-31', 'B-', 'PLASMA',    '2026-09-28', '2026-10-09', '10:00:00'),
(24, '30111245', 'Micaela',   'Suárez',    'Femenino',  '2026-11-12', 'AB-', 'PLATELETS','2026-09-26', '2026-10-19', '09:00:00'),
(25, '30111246', 'Ramiro',    'Aguilar',   'Masculino', '2026-07-07', 'O+', 'BLOOD',     '2026-08-10', '2026-10-19', '10:00:00'),
(26, '30111247', 'Hugo',      'Villalba',  'Masculino', '2026-05-16', 'A+', 'PLASMA',    '2026-10-05', '2026-10-20', '09:00:00'),
(27, '30111248', 'Sebastián', 'Peralta',   'Masculino', '2026-08-14', 'B+', 'PLATELETS','2026-10-01', '2026-10-20', '10:00:00'),
(28, '30111249', 'César',     'Núñez',     'Masculino', '2026-12-20', 'AB+', 'BLOOD',    '2026-08-06', '2026-10-21', '09:00:00'),
(29, '30111250', 'Irene',     'Godoy',     'Femenino',  '2026-10-09', 'O-', 'PLASMA',    '2026-10-04', '2026-10-22', '09:00:00'),
(30, '30111251', 'Candela',   'Barrios',   'Femenino',  '2026-01-19', 'A-', 'PLATELETS','2026-10-02', '2026-10-22', '10:00:00'),
(31, '30111252', 'Leandro',   'Osorio',    'Masculino', '2026-09-04', 'B-', 'BLOOD',     '2026-08-10', '2026-10-23', '09:00:00'),
(32, '30111253', 'Bruno',     'Fuentes',   'Masculino', '2026-06-28', 'AB-', 'PLASMA',   '2026-09-25', '2026-10-23', '10:00:00'),
(33, '30111254', 'Natalia',   'Ruiz',      'Femenino',  '2026-04-02', 'O+', 'PLATELETS','2026-09-28', '2026-10-26', '09:00:00'),
(34, '30111255', 'Esteban',   'Nieto',     'Masculino', '2026-10-26', 'A+', 'BLOOD',     '2026-08-03', '2026-10-26', '10:00:00'),
(35, '30111256', 'Sabrina',   'Cabral',    'Femenino',  '2026-02-23', 'B+', 'PLASMA',    '2026-09-24', '2026-10-27', '09:00:00'),
(36, '30111257', 'Delfina',   'Mendoza',   'Femenino',  '2026-08-08', 'AB+', 'PLATELETS','2026-09-30', '2026-10-28', '09:00:00'),
(37, '30111258', 'Matías',    'Ponce',     'Masculino', '2026-12-18', 'O-', 'BLOOD',     '2026-08-09', '2026-10-28', '10:00:00'),
(38, '30111259', 'Iván',      'Leiva',     'Masculino', '2026-03-25', 'A-', 'PLASMA',    '2026-09-23', '2026-10-29', '09:00:00'),
(39, '30111260', 'Ezequiel',  'Britez',    'Masculino', '2026-05-06', 'B-', 'PLATELETS','2026-10-01', '2026-10-29', '10:00:00'),
(40, '30111261', 'Malena',    'Vargas',    'Femenino',  '2026-11-30', 'AB-', 'BLOOD',    '2026-08-04', '2026-10-30', '09:00:00');

-- ------------------------------------------------------------
-- 1) Donantes
-- ------------------------------------------------------------
INSERT INTO `donors`
  (`dni`, `first_name`, `last_name`, `gender`, `birth_date`, `city`,
   `address`, `blood_type`, `phenotype`, `weight`, `height`, `status`)
SELECT
  s.`dni`, s.`first_name`, s.`last_name`, s.`gender`, s.`birth_date`,
  'Mar del Plata',
  CONCAT('Calle ', 100 + s.`idx`),
  s.`blood_type`,
  IF(s.`idx` % 7 = 0, 'Kell-', NULL),
  IF(s.`gender` = 'Femenino', 56 + (s.`idx` % 8) * 2, 72 + (s.`idx` % 10) * 2),
  IF(s.`gender` = 'Femenino', 158 + (s.`idx` % 6) * 2, 172 + (s.`idx` % 6) * 2),
  'APTO'
FROM `tmp_seed` s;

-- ------------------------------------------------------------
-- 2) Teléfonos (1 primario por donante)
-- ------------------------------------------------------------
INSERT INTO `donor_phones` (`donor_id`, `phone_number`, `phone_type`, `is_primary`)
SELECT d.id, CONCAT('2235', LPAD(100000 + s.`idx` * 17, 6, '0')), 'Mobile', 1
FROM `donors` d JOIN `tmp_seed` s ON d.`dni` = s.`dni`;

-- ------------------------------------------------------------
-- 3) Emails (8 de cada 10 donantes)
-- ------------------------------------------------------------
INSERT INTO `donor_emails` (`donor_id`, `email_address`, `email_type`, `is_primary`)
SELECT d.id, CONCAT(s.`dni`, '@mail.com'), 'Personal', 1
FROM `donors` d JOIN `tmp_seed` s ON d.`dni` = s.`dni`
WHERE s.`idx` % 5 <> 0;

-- ------------------------------------------------------------
-- 4) Turnos en la Agenda (40)
-- ------------------------------------------------------------
INSERT INTO `appointments`
  (`donor_id`, `appointment_date`, `appointment_time`, `target_procedure`, `status`)
SELECT d.id, s.`appt_date`, s.`appt_time`, s.`proc`, 'SCHEDULED'
FROM `donors` d JOIN `tmp_seed` s ON d.`dni` = s.`dni`;

-- ------------------------------------------------------------
-- 5) Donaciones históricas (1 por donante, ids 1001..1040)
--    Carga Estadísticas + last_donation_date.
-- ------------------------------------------------------------
INSERT INTO `donations`
  (`id`, `donor_id`, `weight`, `height`, `hto`, `platelets_count`, `annual_limit_override`, `staff_user_id`, `created_at`)
SELECT
  1000 + s.`idx`, d.id, d.`weight`, d.`height`,
  ELT(1 + (s.`idx` % 9), 35, 36, 37, 38, 39, 40, 41, 42, 43),
  ELT(1 + (s.`idx` % 7), 150000, 180000, 220000, 260000, 300000, 350000, 420000),
  0, NULL, CONCAT(s.`don_date`, ' 10:30:00')
FROM `donors` d JOIN `tmp_seed` s ON d.`dni` = s.`dni`;

INSERT INTO `donation_procedures` (`donation_id`, `procedure_type`)
SELECT 1000 + s.`idx`, s.`proc` FROM `tmp_seed` s;

-- ------------------------------------------------------------
-- 6) last_donation_date + next_eligible_date
--    Cálculo espejo de db_config::calculateEligibleWorkdayReturnDate
--    (sáb → lunes +2, dom → lunes +1), según procedimiento y género.
-- ------------------------------------------------------------
UPDATE `donors` d
JOIN `tmp_seed` s ON d.`dni` = s.`dni`
JOIN `donation_recovery_policies` p ON p.`procedure_type` = s.`proc`
SET d.`last_donation_date` = s.`don_date`,
    d.`next_eligible_date` = (
      SELECT CASE DAYOFWEEK(DATE_ADD(s.`don_date`, INTERVAL IF(d.`gender` = 'Femenino', p.`recovery_days_female`, p.`recovery_days`) DAY))
        WHEN 7 THEN DATE_ADD(DATE_ADD(s.`don_date`, INTERVAL IF(d.`gender` = 'Femenino', p.`recovery_days_female`, p.`recovery_days`) DAY), INTERVAL 2 DAY)
        WHEN 1 THEN DATE_ADD(DATE_ADD(s.`don_date`, INTERVAL IF(d.`gender` = 'Femenino', p.`recovery_days_female`, p.`recovery_days`) DAY), INTERVAL 1 DAY)
        ELSE DATE_ADD(s.`don_date`, INTERVAL IF(d.`gender` = 'Femenino', p.`recovery_days_female`, p.`recovery_days`) DAY)
      END
    );

-- ------------------------------------------------------------
-- 7) Restricciones médicas (5 donantes → EN VEDA por restricción)
-- ------------------------------------------------------------
INSERT INTO `donor_restrictions`
  (`donor_id`, `restriction_id`, `start_date`, `veda_days`,
   `calculated_end_date`, `eligible_workday_return_date`, `notes`, `created_by_user_id`)
SELECT
  d.id, r.id, t.`start_date`, t.`veda_days`,
  DATE_ADD(t.`start_date`, INTERVAL t.`veda_days` DAY),
  (SELECT CASE DAYOFWEEK(DATE_ADD(t.`start_date`, INTERVAL t.`veda_days` DAY))
     WHEN 7 THEN DATE_ADD(DATE_ADD(t.`start_date`, INTERVAL t.`veda_days` DAY), INTERVAL 2 DAY)
     WHEN 1 THEN DATE_ADD(DATE_ADD(t.`start_date`, INTERVAL t.`veda_days` DAY), INTERVAL 1 DAY)
     ELSE DATE_ADD(t.`start_date`, INTERVAL t.`veda_days` DAY) END),
  t.`notes`, NULL
FROM (
       SELECT '30111245' AS `dni`, 'Tatuaje Reciente'    AS `name`, '2026-08-15' AS `start_date`, 180 AS `veda_days`, 'Veda por cita reciente' AS `notes`
  UNION ALL SELECT '30111247', 'Viaje a Zona Endémica', '2026-10-01', 90, ''
  UNION ALL SELECT '30111249', 'Bloqueo por Medicación','2026-09-28', 30, ''
  UNION ALL SELECT '30111251', 'Cirugía Reciente',      '2026-01-15', 365, ''
  UNION ALL SELECT '30111253', 'Tatuaje Reciente',      '2026-09-01', 180, ''
) t
JOIN `donors` d ON d.`dni` = t.`dni`
JOIN `restrictions` r ON r.`name` = t.`name`;

DROP TEMPORARY TABLE IF EXISTS `tmp_seed`;