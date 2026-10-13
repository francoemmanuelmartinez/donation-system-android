-- ============================================================
-- TEST 4AM/5AM (2026-10-11): UNDO / limpieza
-- Borra los donantes + turnos de prueba y restaura los turnos
-- seed del 13/10 a su estado original (is_reminder_sent = 0).
-- ============================================================
SET NAMES utf8mb4;

DELETE a FROM `appointments` a
JOIN `donors` d ON d.`id` = a.`donor_id`
WHERE d.`dni` IN ('99990001', '99990002');

DELETE dp FROM `donor_phones` dp
JOIN `donors` d ON d.`id` = dp.`donor_id`
WHERE d.`dni` IN ('99990001', '99990002');

DELETE d FROM `donors` d
WHERE d.`dni` IN ('99990001', '99990002');

-- Restaurar turnos seed del 13/10 (los que el setup había neutralizado)
UPDATE `appointments` SET `is_reminder_sent` = 0
WHERE `appointment_date` = '2026-10-13';