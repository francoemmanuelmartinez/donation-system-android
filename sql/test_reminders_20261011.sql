-- ============================================================
-- TEST 4AM/5AM (2026-10-11): setup de datos de prueba
-- 2 donantes + 2 turnos el 13/10 (HOY+2) con el teléfono real
-- de prueba (5492235024735). Marca los turnos seed del 13/10
-- como "ya recordados" para que no sean candidatos del dispatcher.
-- Deshacer: sql/undo_test_reminders_20261011.sql
-- ============================================================
SET NAMES utf8mb4;

-- Neutralizar turnos seed del 13/10 (se restauran en el undo)
UPDATE `appointments` SET `is_reminder_sent` = 1
WHERE `appointment_date` = '2026-10-13' AND `is_reminder_sent` = 0;

-- Donantes de prueba (DNI ficticios únicos)
INSERT INTO `donors`
  (`dni`, `first_name`, `last_name`, `gender`, `birth_date`, `city`,
   `address`, `blood_type`, `weight`, `height`, `status`)
VALUES
  ('99990001', 'Testeo', 'Uno',  'Masculino', '1990-01-10', 'Mar del Plata', 'Calle Testeo 1', 'O+', 75.00, 175.00, 'APTO'),
  ('99990002', 'Testeo', 'Dos',  'Femenino',  '1992-02-20', 'Mar del Plata', 'Calle Testeo 2', 'A+', 60.00, 165.00, 'APTO');

-- Teléfono real de prueba (1 primario por donante)
INSERT INTO `donor_phones` (`donor_id`, `phone_number`, `phone_type`, `is_primary`)
SELECT `id`, '5492235024735', 'Mobile', 1 FROM `donors` WHERE `dni` IN ('99990001', '99990002');

-- Turnos: horarios tempranos para quedar primeros en ORDER BY appointment_time
INSERT INTO `appointments` (`donor_id`, `appointment_date`, `appointment_time`, `target_procedure`, `status`)
SELECT `id`, '2026-10-13', '06:00:00', 'PLASMA',    'SCHEDULED' FROM `donors` WHERE `dni` = '99990001';

INSERT INTO `appointments` (`donor_id`, `appointment_date`, `appointment_time`, `target_procedure`, `status`)
SELECT `id`, '2026-10-13', '06:30:00', 'PLATELETS', 'SCHEDULED' FROM `donors` WHERE `dni` = '99990002';