-- Migration: appointments lifecycle + audit (Agenda robusta, P1)
-- For existing databases. Idempotent where MariaDB supports it.
-- Adds NO_SHOW status, audit columns, reschedule traceability and the
-- donation <-> appointment link. See plan-agenda-robusta.md (T1).

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) Extend status ENUM with NO_SHOW (keep existing values/order)
ALTER TABLE `appointments`
  MODIFY COLUMN `status` ENUM('SCHEDULED', 'ATTENDED', 'CANCELLED', 'NO_SHOW') NOT NULL DEFAULT 'SCHEDULED';

-- 2) Audit + traceability columns
ALTER TABLE `appointments`
  ADD COLUMN `cancelled_at` DATETIME NULL AFTER `status`,
  ADD COLUMN `cancelled_by_user_id` INT NULL AFTER `cancelled_at`,
  ADD COLUMN `cancel_reason` VARCHAR(120) NULL AFTER `cancelled_by_user_id`,
  ADD COLUMN `rescheduled_to_appointment_id` INT NULL AFTER `cancel_reason`,
  ADD COLUMN `reschedule_requested_at` DATETIME NULL AFTER `rescheduled_to_appointment_id`,
  ADD COLUMN `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`,
  ADD CONSTRAINT `fk_appointments_cancelled_by` FOREIGN KEY (`cancelled_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_appointments_rescheduled_to` FOREIGN KEY (`rescheduled_to_appointment_id`) REFERENCES `appointments`(`id`) ON DELETE SET NULL;

-- 3) Donation <-> appointment link
ALTER TABLE `donations`
  ADD COLUMN `appointment_id` INT NULL AFTER `center_id`,
  ADD CONSTRAINT `fk_donations_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments`(`id`) ON DELETE SET NULL;

-- 4) Index for active-per-center queries
CREATE INDEX IF NOT EXISTS `idx_appointments_center_status_date`
    ON `appointments` (`center_id`, `status`, `appointment_date`);

SET FOREIGN_KEY_CHECKS = 1;
