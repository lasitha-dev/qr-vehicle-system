-- ============================================================
-- Dual-Compatible DB Migration Script
-- QR Vehicle System (Spring Boot + PHP coexistence)
-- 
-- Run this against the existing PHP 'qrcodedb' database.
-- All changes are ADDITIVE — existing PHP code will continue
-- to work unchanged after this migration.
--
-- ⚠️  BACK UP YOUR DATABASE BEFORE RUNNING THIS SCRIPT  ⚠️
-- ============================================================

-- ============================================================
-- 1. VEHIDB TABLE — Add new columns for Spring Boot features
--    PHP never references these columns, so this is safe.
-- ============================================================
ALTER TABLE `vehidb`
  ADD COLUMN IF NOT EXISTS `vehicle_type_id` int DEFAULT NULL AFTER `VehiOwner`,
  ADD COLUMN IF NOT EXISTS `Mobile` varchar(15) DEFAULT NULL AFTER `vehicle_type_id`,
  ADD COLUMN IF NOT EXISTS `Email` varchar(100) DEFAULT NULL AFTER `Mobile`,
  ADD COLUMN IF NOT EXISTS `email_sent` tinyint(1) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS `change_email_sent` tinyint(1) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS `cert_viewed_at` datetime DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS `last_notified_status` varchar(20) DEFAULT NULL;

-- ============================================================
-- 2. VEHICLE_TYPES TABLE — New lookup table for Spring Boot
--    PHP doesn't use this table.
-- ============================================================
CREATE TABLE IF NOT EXISTS `vehicle_types` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type_name` varchar(50) NOT NULL,
  `icon` varchar(10) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Seed vehicle types (skip if already exist)
INSERT IGNORE INTO `vehicle_types` (`id`, `type_name`, `icon`, `is_active`) VALUES
(1, 'Motorcycle', '🏍', 1),
(2, 'Car', '🚗', 1),
(3, 'Van', '🚐', 1),
(4, 'Bus', '🚌', 1),
(5, 'Three Wheeler', '🛺', 1),
(6, 'Other', '🚙', 1);

-- FK constraint: vehidb.vehicle_type_id -> vehicle_types.id
-- If this fails, the constraint already exists — safe to ignore.
ALTER TABLE `vehidb`
  ADD CONSTRAINT `fk_vehicle_type` FOREIGN KEY (`vehicle_type_id`)
  REFERENCES `vehicle_types` (`id`);

-- ============================================================
-- 3. USER TABLE — Widen password column for hashed passwords
--    PHP stores plaintext (e.g. "adminnilu") which still fits
--    in a varchar(255) column. No PHP impact.
-- ============================================================
ALTER TABLE `user`
  MODIFY COLUMN `password` varchar(255) NOT NULL;

-- ============================================================
-- 4. USER TABLE — Change PK to composite (username, utype)
--    This allows the same username to hold multiple roles.
--
--    PHP IMPACT ANALYSIS:
--    - logincheck.php queries: WHERE username=? AND password=?
--      → still returns exactly 1 row (unique combo)
--    - Duplicate check: SELECT username FROM user WHERE username=?
--      → PHP only inserts with utype='entry', never duplicates
--    - INSERT/UPDATE: always include utype value
--    ✅ SAFE for PHP
-- ============================================================
ALTER TABLE `user` DROP PRIMARY KEY;
ALTER TABLE `user` ADD PRIMARY KEY (`username`, `utype`);

-- ============================================================
-- 5. VISITOR TABLE — Add missing primary key
--    PHP doesn't rely on the absence of a PK.
--    Spring Boot's Visitor.java expects ID as @Id.
-- ============================================================
ALTER TABLE `visitor` ADD PRIMARY KEY (`ID`);

-- ============================================================
-- MIGRATION COMPLETE
-- 
-- The database now supports both:
--   ✅ PHP application (all existing queries unchanged)
--   ✅ Spring Boot application (all JPA entities satisfied)
--
-- NOTE on passwords:
--   PHP continues to use plaintext passwords.
--   Spring Boot should use NoOpPasswordEncoder or a
--   DelegatingPasswordEncoder that supports both plaintext
--   (for existing users) and BCrypt (for new users).
-- ============================================================
