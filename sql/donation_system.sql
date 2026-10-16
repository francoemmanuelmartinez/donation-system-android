-- Donation System (Plasma & Platelets Bank) Schema
-- Database: donation_system

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `donation_procedures`;
DROP TABLE IF EXISTS `donations`;
DROP TABLE IF EXISTS `donation_recovery_policies`;
DROP TABLE IF EXISTS `appointments`;
DROP TABLE IF EXISTS `donor_restrictions`;
DROP TABLE IF EXISTS `restrictions`;
DROP TABLE IF EXISTS `donor_emails`;
DROP TABLE IF EXISTS `donor_phones`;
DROP TABLE IF EXISTS `donors`;
DROP TABLE IF EXISTS `user_emails`;
DROP TABLE IF EXISTS `user_phones`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `reminder_dispatch_control`;
SET FOREIGN_KEY_CHECKS = 1;

-- --------------------------------------------------------
-- Table: users (Staff Accounts)
-- --------------------------------------------------------
CREATE TABLE `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `first_name` VARCHAR(100) NOT NULL,
  `last_name` VARCHAR(100) NOT NULL,
  `dni` VARCHAR(20) NOT NULL UNIQUE,
  `email` VARCHAR(150) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` ENUM('NONE', 'ADMINISTRATIVA', 'DIRECTORA') NOT NULL DEFAULT 'NONE',
  `is_approved` TINYINT(1) NOT NULL DEFAULT 0,
  `approved_by_user_id` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`approved_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: user_phones
-- --------------------------------------------------------
CREATE TABLE `user_phones` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `phone_number` VARCHAR(30) NOT NULL,
  `phone_type` VARCHAR(20) NOT NULL DEFAULT 'Mobile',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: user_emails
-- --------------------------------------------------------
CREATE TABLE `user_emails` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `email_address` VARCHAR(150) NOT NULL,
  `email_type` VARCHAR(20) NOT NULL DEFAULT 'Personal',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: donors
-- --------------------------------------------------------
CREATE TABLE `donors` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `dni` VARCHAR(20) NOT NULL UNIQUE,
  `first_name` VARCHAR(100) NOT NULL,
  `last_name` VARCHAR(100) NOT NULL,
  `gender` VARCHAR(20) NOT NULL DEFAULT 'Otro',
  `birth_date` DATE NOT NULL,
  `city` VARCHAR(100) NOT NULL,
  `zip_code` VARCHAR(20) NOT NULL,
  `address` VARCHAR(150) NOT NULL,
  `neighborhood` VARCHAR(100) NOT NULL,
  `blood_type` VARCHAR(10) NOT NULL,
  `phenotype` VARCHAR(100) NULL,
  `weight` DECIMAL(5,2) NULL,
  `height` DECIMAL(5,2) NULL,
  `status` ENUM('APTO', 'EN_VEDA') NOT NULL DEFAULT 'APTO',
  `last_donation_date` DATE NULL,
  `next_eligible_date` DATE NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: donor_phones
-- --------------------------------------------------------
CREATE TABLE `donor_phones` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donor_id` INT NOT NULL,
  `phone_number` VARCHAR(30) NOT NULL,
  `phone_type` VARCHAR(20) NOT NULL DEFAULT 'Mobile',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: donor_emails
-- --------------------------------------------------------
CREATE TABLE `donor_emails` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donor_id` INT NOT NULL,
  `email_address` VARCHAR(150) NOT NULL,
  `email_type` VARCHAR(20) NOT NULL DEFAULT 'Personal',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: restrictions (Deferral Dictionary)
-- --------------------------------------------------------
CREATE TABLE `restrictions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE,
  `default_veda_days` INT NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial Pre-populated Medical Deferral Dictionary
INSERT INTO `restrictions` (`name`, `default_veda_days`) VALUES
('Tatuaje Reciente', 180),
('Cirugía Reciente', 365),
('Bloqueo por Medicación', 30),
('Viaje a Zona Endémica', 90);

-- --------------------------------------------------------
-- Table: donor_restrictions (Donor Medical Deferral Logs)
-- --------------------------------------------------------
CREATE TABLE `donor_restrictions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donor_id` INT NOT NULL,
  `restriction_id` INT NOT NULL,
  `start_date` DATE NOT NULL,
  `veda_days` INT NULL,
  `is_permanent` TINYINT(1) NOT NULL DEFAULT 0,
  `calculated_end_date` DATE NULL,
  `eligible_workday_return_date` DATE NULL,
  `notes` TEXT NULL,
  `created_by_user_id` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`restriction_id`) REFERENCES `restrictions`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: donation_recovery_policies (Recovery from donation)
-- Configurable by the Director. INCUCAI/SAS baseline values:
--   PLASMA:   2 days  / 25 per year
--   PLATELETS: 7 days / 12 per year
--   BLOOD:    60 days (male) / 90 (female) / 4 (male) / 3 (female) per year
-- --------------------------------------------------------
CREATE TABLE `donation_recovery_policies` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `procedure_type` ENUM('PLASMA', 'PLATELETS', 'BLOOD') NOT NULL UNIQUE,
  `recovery_days` INT NOT NULL,
  `recovery_days_female` INT NOT NULL,
  `max_per_year` INT NOT NULL,
  `max_per_year_female` INT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `donation_recovery_policies` (`procedure_type`, `recovery_days`, `recovery_days_female`, `max_per_year`, `max_per_year_female`) VALUES
('PLASMA', 2, 2, 25, 25),
('PLATELETS', 7, 7, 12, 12),
('BLOOD', 60, 90, 4, 3);

-- --------------------------------------------------------
-- Table: donations
-- --------------------------------------------------------
CREATE TABLE `donations` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donor_id` INT NOT NULL,
  `weight` DECIMAL(5,2) NOT NULL DEFAULT 0,
  `height` DECIMAL(5,2) NOT NULL DEFAULT 0,
  `hto` DECIMAL(4,1) NOT NULL,
  `platelets_count` INT NOT NULL,
  `annual_limit_override` TINYINT(1) NOT NULL DEFAULT 0,
  `staff_user_id` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`staff_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: donation_procedures
-- --------------------------------------------------------
CREATE TABLE `donation_procedures` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donation_id` INT NOT NULL,
  `procedure_type` ENUM('PLASMA', 'PLATELETS', 'BLOOD') NOT NULL,
  FOREIGN KEY (`donation_id`) REFERENCES `donations`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: appointments
-- --------------------------------------------------------
CREATE TABLE `appointments` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donor_id` INT NOT NULL,
  `appointment_date` DATE NOT NULL,
  `appointment_time` TIME NOT NULL,
  `target_procedure` VARCHAR(50) NOT NULL,
  `status` ENUM('SCHEDULED', 'ATTENDED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  `is_reminder_sent` TINYINT(1) NOT NULL DEFAULT 0,
  `is_email_reminder_sent` TINYINT(1) NOT NULL DEFAULT 0,
  `reminder_sent_at` DATETIME NULL,
  `reminder_sent_by_user_id` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`reminder_sent_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: reminder_dispatch_control (Automatic reminder dispatch guard)
-- Single-row table used by the lazy sweep + warm-up of send_reminders.php
-- (see sql/migration_reminders_control.sql).
-- --------------------------------------------------------
CREATE TABLE `reminder_dispatch_control` (
  `id` INT NOT NULL DEFAULT 1 PRIMARY KEY,
  `last_run_at` DATETIME NULL,
  `warmup_start_date` DATE NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `reminder_dispatch_control` (`id`, `last_run_at`, `warmup_start_date`)
VALUES (1, NULL, NULL);

-- --------------------------------------------------------
-- Indexes for the unified search module (scalability)
-- (see sql/migration_search_indexes.sql)
-- NOTE: CREATE INDEX IF NOT EXISTS requires MariaDB (XAMPP); on plain
-- MySQL import this section after removing the IF NOT EXISTS clauses.
-- --------------------------------------------------------
CREATE INDEX IF NOT EXISTS `idx_donors_blood_type` ON `donors` (`blood_type`);
CREATE INDEX IF NOT EXISTS `idx_donors_name` ON `donors` (`last_name`, `first_name`);
CREATE INDEX IF NOT EXISTS `idx_donors_next_eligible` ON `donors` (`next_eligible_date`);
CREATE INDEX IF NOT EXISTS `idx_donor_restrictions_active`
    ON `donor_restrictions` (`donor_id`, `eligible_workday_return_date`);
CREATE INDEX IF NOT EXISTS `idx_appointments_scheduled`
    ON `appointments` (`donor_id`, `status`, `appointment_date`);

-- --------------------------------------------------------
-- Initial Seed Data: Default Approved Accounts
-- Password director@clinic.org: "password" (BCRYPT hash)
-- Password admin: "admin" (BCRYPT hash)
-- --------------------------------------------------------
INSERT INTO `users` (`id`, `first_name`, `last_name`, `dni`, `email`, `password_hash`, `role`, `is_approved`) VALUES
(1, 'Directora', 'General', '00000000', 'director@clinic.org', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'DIRECTORA', 1),
(2, 'Admin', 'Admin', '11111111', 'admin', '$2y$10$8mw30qAmG6yCjygTJcOEvOp3OdWlYMoFpXEUWqwh8eNa5zkM9Lj8K', 'DIRECTORA', 1);
