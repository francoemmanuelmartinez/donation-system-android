-- Migración: control de dispatch de recordatorios automáticos (lazy sweep + warm-up)
-- Ejecutar una sola vez sobre la BD donation_system:
--   mysql -u root donation_system < migration_reminders_control.sql

CREATE TABLE IF NOT EXISTS `reminder_dispatch_control` (
  `id` INT NOT NULL DEFAULT 1 PRIMARY KEY,
  `last_run_at` DATETIME NULL,
  `warmup_start_date` DATE NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `reminder_dispatch_control` (`id`, `last_run_at`, `warmup_start_date`)
VALUES (1, NULL, NULL)
ON DUPLICATE KEY UPDATE `id` = VALUES(`id`);