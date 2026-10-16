-- Migración: veda permanente en donor_restrictions
-- Ejecutar una sola vez sobre la BD donation_system:
--   mysql -u root donation_system < migration_permanent_veda.sql
-- Cambios:
--   * is_permanent marca una veda que nunca vence (veda_days / fechas quedan NULL).
--   * Las columnas de veda se vuelven NULLables para soportar el caso permanente.
-- Nota: el índice idx_donor_restrictions_active se mantiene igual (sirve además
-- a la FK sobre donor_id); el filtro de "restricción activa"
-- (is_permanent = 1 OR eligible_workday_return_date >= CURDATE()) queda a cargo
-- de la condición OR, no del rango del índice.

ALTER TABLE `donor_restrictions`
  ADD `is_permanent` TINYINT(1) NOT NULL DEFAULT 0 AFTER `veda_days`,
  MODIFY `veda_days` INT NULL,
  MODIFY `calculated_end_date` DATE NULL,
  MODIFY `eligible_workday_return_date` DATE NULL;
