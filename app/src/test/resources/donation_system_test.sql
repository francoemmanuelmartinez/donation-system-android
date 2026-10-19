-- Donation System (Plasma & Platelets Bank) Schema - TEST FIXTURE
-- Mirrors sql/donation_system.sql. Applied by TestDatabase against donation_system_test.

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
SET FOREIGN_KEY_CHECKS = 1;

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

CREATE TABLE `user_phones` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `phone_number` VARCHAR(30) NOT NULL,
  `phone_type` VARCHAR(20) NOT NULL DEFAULT 'Mobile',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_emails` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `email_address` VARCHAR(150) NOT NULL,
  `email_type` VARCHAR(20) NOT NULL DEFAULT 'Personal',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `donors` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `dni` VARCHAR(20) NOT NULL UNIQUE,
  `first_name` VARCHAR(100) NOT NULL,
  `last_name` VARCHAR(100) NOT NULL,
  `gender` VARCHAR(20) NOT NULL DEFAULT 'Otro',
  `birth_date` DATE NOT NULL,
  `city` VARCHAR(100) NOT NULL,
  `address` VARCHAR(150) NOT NULL,
  `blood_type` VARCHAR(10) NOT NULL,
  `phenotype` VARCHAR(100) NULL,
  `weight` DECIMAL(5,2) NULL,
  `height` DECIMAL(5,2) NULL,
  `last_donation_date` DATE NULL,
  `next_eligible_date` DATE NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `donor_phones` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donor_id` INT NOT NULL,
  `phone_number` VARCHAR(30) NOT NULL,
  `phone_type` VARCHAR(20) NOT NULL DEFAULT 'Mobile',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `donor_emails` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donor_id` INT NOT NULL,
  `email_address` VARCHAR(150) NOT NULL,
  `email_type` VARCHAR(20) NOT NULL DEFAULT 'Personal',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`donor_id`) REFERENCES `donors`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `restrictions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE,
  `default_veda_days` INT NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `restrictions` (`name`, `default_veda_days`) VALUES
('Tatuaje Reciente', 180),
('Cirugía Reciente', 365),
('Bloqueo por Medicación', 30),
('Viaje a Zona Endémica', 90);

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

CREATE TABLE `donation_procedures` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `donation_id` INT NOT NULL,
  `procedure_type` ENUM('PLASMA', 'PLATELETS', 'BLOOD') NOT NULL,
  FOREIGN KEY (`donation_id`) REFERENCES `donations`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

-- Default approved director. Password: "password" (well-known BCrypt test vector).
INSERT INTO `users` (`id`, `first_name`, `last_name`, `dni`, `email`, `password_hash`, `role`, `is_approved`) VALUES
(1, 'Directora', 'Test', '00000000', 'director@test.local', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'DIRECTORA', 1);