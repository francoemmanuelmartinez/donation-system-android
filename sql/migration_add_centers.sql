-- Migration: add centers table + center_id columns (Enfoque A)
-- For existing databases. Safe to run multiple times (uses IF NOT EXISTS / IF EXISTS where supported).

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) Create centers table
CREATE TABLE IF NOT EXISTS `centers` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `address` VARCHAR(150) NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default center
INSERT INTO `centers` (`id`, `name`, `address`)
SELECT 1, 'Centro Principal', 'Mar del Plata'
WHERE NOT EXISTS (SELECT 1 FROM `centers` WHERE `id` = 1);

-- 2) Add center_id to users
ALTER TABLE `users`
  ADD COLUMN `center_id` INT NULL AFTER `approved_by_user_id`,
  ADD CONSTRAINT `fk_users_center` FOREIGN KEY (`center_id`) REFERENCES `centers`(`id`) ON DELETE SET NULL;

-- 3) Add center_id to donors
ALTER TABLE `donors`
  ADD COLUMN `center_id` INT NULL AFTER `height`,
  ADD CONSTRAINT `fk_donors_center` FOREIGN KEY (`center_id`) REFERENCES `centers`(`id`) ON DELETE SET NULL;

-- 4) Add center_id to donor_restrictions
ALTER TABLE `donor_restrictions`
  ADD COLUMN `center_id` INT NULL AFTER `created_by_user_id`,
  ADD CONSTRAINT `fk_donor_restrictions_center` FOREIGN KEY (`center_id`) REFERENCES `centers`(`id`) ON DELETE SET NULL;

-- 5) Add center_id to donations
ALTER TABLE `donations`
  ADD COLUMN `center_id` INT NULL AFTER `staff_user_id`,
  ADD CONSTRAINT `fk_donations_center` FOREIGN KEY (`center_id`) REFERENCES `centers`(`id`) ON DELETE SET NULL;

-- 6) Add center_id to appointments
ALTER TABLE `appointments`
  ADD COLUMN `center_id` INT NULL AFTER `reminder_sent_by_user_id`,
  ADD CONSTRAINT `fk_appointments_center` FOREIGN KEY (`center_id`) REFERENCES `centers`(`id`) ON DELETE SET NULL;

-- 7) Backfill: assign all existing data to center 1
UPDATE `users` SET `center_id` = 1 WHERE `center_id` IS NULL;
UPDATE `donors` SET `center_id` = 1 WHERE `center_id` IS NULL;
UPDATE `donor_restrictions` SET `center_id` = 1 WHERE `center_id` IS NULL;
UPDATE `donations` SET `center_id` = 1 WHERE `center_id` IS NULL;
UPDATE `appointments` SET `center_id` = 1 WHERE `center_id` IS NULL;

-- 8) Update role ENUM (remove NONE, add SUPERADMIN)
ALTER TABLE `users`
  MODIFY COLUMN `role` ENUM('ADMINISTRATIVA', 'DIRECTORA', 'SUPERADMIN') NOT NULL DEFAULT 'ADMINISTRATIVA';

-- Migrate existing NONE roles to ADMINISTRATIVA
UPDATE `users` SET `role` = 'ADMINISTRATIVA' WHERE `role` = 'NONE';

SET FOREIGN_KEY_CHECKS = 1;
