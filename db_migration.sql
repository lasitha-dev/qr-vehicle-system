-- ============================================================
-- DB Migration Script for QR Vehicle System (Spring Boot)
-- Run this against the 'vehicle_qr_db' database
-- ============================================================

-- 1. Add missing columns to vehidb (if using the older schema)
--    Skip if you already imported qrcodedb (1).sql
ALTER TABLE `vehidb`
  ADD COLUMN IF NOT EXISTS `vehicle_type_id` int DEFAULT NULL AFTER `VehiOwner`,
  ADD COLUMN IF NOT EXISTS `Mobile` varchar(12) DEFAULT NULL AFTER `vehicle_type_id`,
  ADD COLUMN IF NOT EXISTS `Email` text DEFAULT NULL AFTER `Mobile`,
  ADD COLUMN IF NOT EXISTS `email_sent` tinyint(1) DEFAULT '0',
  ADD COLUMN IF NOT EXISTS `change_email_sent` tinyint(1) DEFAULT '0',
  ADD COLUMN IF NOT EXISTS `cert_viewed_at` datetime DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `last_notified_status` varchar(20) DEFAULT NULL;

-- 2. Create vehicle_types table (if not already created via vehicle_types.sql)
CREATE TABLE IF NOT EXISTS `vehicle_types` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type_name` varchar(50) NOT NULL,
  `icon` varchar(10) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 3. Seed vehicle types (ignore if already exist)
INSERT IGNORE INTO `vehicle_types` (`id`, `type_name`, `icon`, `is_active`) VALUES
(1, 'Motorcycle', '🏍', 1),
(2, 'Car', '🚗', 1),
(3, 'Van', '🚐', 1),
(4, 'Bus', '🚌', 1),
(5, 'Three Wheeler', '🛺', 1),
(6, 'Other', '🚙', 1);

-- 4. Add FK constraint for vehicle_type_id (if not exists)
--    If this fails, it already exists — safe to ignore the error.
ALTER TABLE `vehidb`
  ADD CONSTRAINT `fk_vehicle_type` FOREIGN KEY (`vehicle_type_id`) REFERENCES `vehicle_types` (`id`);
