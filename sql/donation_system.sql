-- Donation System (Plasma & Platelets Bank) Schema
-- Database: donation_system

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `donation_procedures`;
DROP TABLE IF EXISTS `donations`;
DROP TABLE IF EXISTS `appointments`;
DROP TABLE IF EXISTS `donor_restrictions`;
DROP TABLE IF EXISTS `restrictions`;
DROP TABLE IF EXISTS `donor_emails`;
DROP TABLE IF EXISTS `donor_phones`;
DROP TABLE IF EXISTS `donors`;
DROP TABLE IF EXISTS `user_emails`;
DROP TABLE IF EXISTS `user_phones`;
DROP TABLE IF EXISTS `users`;
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
  `status` ENUM('APTO', 'EN_VEDA') NOT NULL DEFAULT 'APTO',
  `last_donation_date` DATE NULL,
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
  `veda_days` INT NOT NULL,
  `calculated_end_date` DATE NOT NULL,
  `eligible_workday_return_date` DATE NOT NULL,
  `notes` TEXT NULL,
  `created_by_user_id` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`restriction_id`) REFERENCES `restrictions`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`created_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table: donations
-- --------------------------------------------------------
CREATE TABLE `donations` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donor_id` INT NOT NULL,
  `weight` DECIMAL(5,2) NOT NULL,
  `height` DECIMAL(5,2) NOT NULL,
  `hto` DECIMAL(4,1) NOT NULL,
  `platelets_count` INT NOT NULL,
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
  `reminder_sent_at` DATETIME NULL,
  `reminder_sent_by_user_id` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`reminder_sent_by_user_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Initial Seed Data: Default Approved Director Account
-- Password: "adminpassword" (BCRYPT hash)
-- --------------------------------------------------------
INSERT INTO `users` (`id`, `first_name`, `last_name`, `dni`, `email`, `password_hash`, `role`, `is_approved`) VALUES
(1, 'Directora', 'General', '00000000', 'director@clinic.org', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'DIRECTORA', 1);
