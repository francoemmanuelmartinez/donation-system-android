-- Migración: índices para el módulo único de búsqueda (escalabilidad)
-- Ejecutar una sola vez sobre la BD donation_system:
--   mysql -u root donation_system < migration_search_indexes.sql

CREATE INDEX IF NOT EXISTS `idx_donors_blood_type` ON `donors` (`blood_type`);
CREATE INDEX IF NOT EXISTS `idx_donors_name` ON `donors` (`last_name`, `first_name`);
CREATE INDEX IF NOT EXISTS `idx_donors_next_eligible` ON `donors` (`next_eligible_date`);

CREATE INDEX IF NOT EXISTS `idx_donor_restrictions_active`
    ON `donor_restrictions` (`donor_id`, `eligible_workday_return_date`);

CREATE INDEX IF NOT EXISTS `idx_appointments_scheduled`
    ON `appointments` (`donor_id`, `status`, `appointment_date`);
