-- ============================================================
-- studdb initialization script
-- Creates the studdb database and all tables matching the
-- real university student database schema (studdb (7).sql)
-- Plus seed data for faculty, course, district and test student
-- ============================================================

CREATE DATABASE IF NOT EXISTS `studdb` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE `studdb`;

SET NAMES utf8;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

-- -----------------------------------------------------------
-- Table: cancel
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `cancel`;
CREATE TABLE `cancel` (
  `Reg_No` varchar(20) NOT NULL,
  `CancelDate` date NOT NULL,
  `Remark` varchar(500) NOT NULL,
  `User` varchar(20) NOT NULL,
  `Crdate` date NOT NULL,
  `Crtime` time NOT NULL,
  PRIMARY KEY (`Reg_No`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- -----------------------------------------------------------
-- Table: course
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `Course_ID` int(10) NOT NULL DEFAULT '0',
  `Course_name` varchar(38) DEFAULT NULL,
  `Fac_Code` int(2) DEFAULT NULL,
  PRIMARY KEY (`Course_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------------
-- Table: defer
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `defer`;
CREATE TABLE `defer` (
  `Reg_No` varchar(20) NOT NULL,
  `DeferFrom` date NOT NULL DEFAULT '0000-00-00',
  `DeferTo` date DEFAULT NULL,
  `Reason` varchar(500) DEFAULT NULL,
  `User` varchar(20) DEFAULT NULL,
  `Crdate` date DEFAULT NULL,
  `Crtime` time DEFAULT NULL,
  PRIMARY KEY (`Reg_No`,`DeferFrom`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- -----------------------------------------------------------
-- Table: district
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `district`;
CREATE TABLE `district` (
  `Dist_No` int(2) DEFAULT NULL,
  `District` varchar(12) DEFAULT NULL,
  `Province` varchar(13) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------------
-- Table: faculty
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `faculty`;
CREATE TABLE `faculty` (
  `Fac_Code` int(10) NOT NULL DEFAULT '0',
  `Fac_name` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`Fac_Code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------------
-- Table: promote
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `promote`;
CREATE TABLE `promote` (
  `Reg_No` varchar(20) NOT NULL,
  `NewReg_No` varchar(20) NOT NULL,
  `PromotionDate` date NOT NULL,
  `Remark` varchar(500) NOT NULL,
  `PreStatus` varchar(15) NOT NULL,
  `Cancelled` varchar(1) NOT NULL DEFAULT 'N',
  `Reason` varchar(250) DEFAULT NULL,
  `User` varchar(20) NOT NULL,
  `Crdate` date NOT NULL,
  `Crtime` time NOT NULL,
  PRIMARY KEY (`Reg_No`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- -----------------------------------------------------------
-- Table: stud  (student registration records)
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `stud`;
CREATE TABLE `stud` (
  `NIC` varchar(12) NOT NULL,
  `Reg_No` varchar(20) NOT NULL DEFAULT '',
  `Incno` int(5) DEFAULT NULL,
  `App_Year` int(4) NOT NULL,
  `Faculty` int(2) NOT NULL,
  `Course` int(2) NOT NULL,
  `Status` varchar(15) DEFAULT 'NOT REGISTERED',
  `Idrequried` tinyint(1) NOT NULL DEFAULT '0',
  `Idprinted` tinyint(1) NOT NULL DEFAULT '0',
  `User` varchar(20) NOT NULL,
  `Crdate` date NOT NULL,
  `Crtime` time NOT NULL,
  PRIMARY KEY (`NIC`,`Reg_No`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------------
-- Table: studbasic  (student personal info)
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `studbasic`;
CREATE TABLE `studbasic` (
  `NIC` varchar(12) NOT NULL,
  `Index_No` int(10) DEFAULT '0',
  `Title` varchar(4) DEFAULT NULL,
  `L_Name` varchar(100) DEFAULT NULL,
  `Initials` varchar(25) DEFAULT NULL,
  `Full_Name` varchar(255) DEFAULT NULL,
  `Gender` varchar(1) DEFAULT '-',
  `ADD1` varchar(200) DEFAULT NULL,
  `ADD2` varchar(200) DEFAULT NULL,
  `ADD3` varchar(200) DEFAULT NULL,
  `Phone_No` varchar(11) DEFAULT NULL,
  `SelectType` varchar(30) DEFAULT NULL,
  `RegOn` date NOT NULL DEFAULT '0000-00-00',
  `Category` varchar(7) NOT NULL DEFAULT 'LOCAL',
  `Country` varchar(15) NOT NULL DEFAULT '-',
  `RegFee` decimal(6,2) NOT NULL DEFAULT '0.00',
  `FeePaidOn` date NOT NULL DEFAULT '0000-00-00',
  `Bank` varchar(20) DEFAULT NULL,
  `User` varchar(20) NOT NULL,
  `Crdate` date NOT NULL,
  `Crtime` time NOT NULL,
  UNIQUE KEY `NIC` (`NIC`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------------
-- Table: studclass  (semester registrations)
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `studclass`;
CREATE TABLE `studclass` (
  `Reg_No` varchar(20) NOT NULL,
  `Semester` int(3) NOT NULL,
  `RegDate` date NOT NULL,
  `User` varchar(25) NOT NULL,
  `CrDate` date NOT NULL,
  `CrTime` time NOT NULL,
  PRIMARY KEY (`Reg_No`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------------
-- Table: studlog
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `studlog`;
CREATE TABLE `studlog` (
  `username` varchar(20) NOT NULL,
  `password` varchar(25) NOT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- -----------------------------------------------------------
-- Table: studother  (additional student info)
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `studother`;
CREATE TABLE `studother` (
  `NIC` varchar(12) NOT NULL,
  `DOB` date DEFAULT '0000-00-00',
  `Marital` varchar(10) DEFAULT '-',
  `Religion` varchar(20) DEFAULT '-',
  `Ethic` varchar(20) DEFAULT '-',
  `Nationality` varchar(50) DEFAULT NULL,
  `Dist_No` int(2) DEFAULT '0',
  `Z_Score` decimal(5,4) DEFAULT '0.0000',
  `Medium` varchar(1) DEFAULT '-',
  `LastSchool` varchar(75) DEFAULT NULL,
  `Home` varchar(11) DEFAULT NULL,
  `Mobile` varchar(11) DEFAULT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `CAdd1` varchar(200) DEFAULT NULL,
  `CAdd2` varchar(200) DEFAULT NULL,
  `CAdd3` varchar(200) DEFAULT NULL,
  `PName` varchar(100) DEFAULT NULL,
  `PAdd` varchar(200) DEFAULT NULL,
  `PTelNo` varchar(11) DEFAULT NULL,
  `PRelationship` varchar(20) DEFAULT NULL,
  `EName` varchar(100) DEFAULT NULL,
  `ETelNo` varchar(11) DEFAULT NULL,
  `ERelationship` varchar(20) DEFAULT NULL,
  `Remarks` varchar(1000) DEFAULT NULL,
  `PDist` varchar(100) DEFAULT 'UNKNOWN',
  `Div_No` int(3) DEFAULT NULL,
  `Police` varchar(100) DEFAULT NULL,
  `WApp` varchar(11) DEFAULT NULL,
  `FirstGen` varchar(3) DEFAULT NULL,
  `Biology` varchar(2) DEFAULT NULL,
  `Chemistry` varchar(2) DEFAULT NULL,
  `Physics` varchar(2) DEFAULT NULL,
  `UniEmail` varchar(500) DEFAULT NULL,
  `User` varchar(20) NOT NULL,
  `Crdate` date NOT NULL,
  `Crtime` time NOT NULL,
  UNIQUE KEY `NIC` (`NIC`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- -----------------------------------------------------------
-- Table: suspend
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `suspend`;
CREATE TABLE `suspend` (
  `NIC` varchar(12) NOT NULL,
  `SuspendFrom` date NOT NULL,
  `SuspendTo` varchar(100) NOT NULL,
  `Reason` varchar(500) NOT NULL,
  `Deleted` varchar(1) NOT NULL DEFAULT 'N',
  `User` varchar(20) NOT NULL,
  `Crdate` date NOT NULL,
  `Crtime` time NOT NULL,
  PRIMARY KEY (`NIC`,`SuspendFrom`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- -----------------------------------------------------------
-- Table: transfer
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `transfer`;
CREATE TABLE `transfer` (
  `Reg_No` varchar(20) NOT NULL,
  `University` varchar(150) NOT NULL,
  `TransferDate` date NOT NULL,
  `Remark` varchar(500) NOT NULL,
  `CurrentStatus` varchar(20) NOT NULL,
  `Cancelled` varchar(1) NOT NULL DEFAULT 'N',
  `User` varchar(20) NOT NULL,
  `Crdate` date NOT NULL,
  `Crtime` time NOT NULL,
  PRIMARY KEY (`Reg_No`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


-- ============================================================
-- SEED DATA: Faculty
-- Fac_Code values correspond to integer codes used in stud.Faculty
-- Faculty prefixes in Reg_No (A, AG, AHS, D, E, M, MG, S, VS)
-- are parsed from the Reg_No string, not from Fac_Code directly
-- ============================================================
INSERT INTO `faculty` (`Fac_Code`, `Fac_name`) VALUES
(1, 'Faculty of Arts'),
(2, 'Faculty of Agriculture'),
(3, 'Faculty of Allied Health Sciences'),
(4, 'Faculty of Dental Sciences'),
(5, 'Faculty of Engineering'),
(6, 'Faculty of Medicine'),
(7, 'Faculty of Management'),
(8, 'Faculty of Science'),
(9, 'Faculty of Veterinary Medicine & Animal Science');

-- ============================================================
-- SEED DATA: Course
-- ============================================================
INSERT INTO `course` (`Course_ID`, `Course_name`, `Fac_Code`) VALUES
(1, 'Bachelor of Arts', 1),
(2, 'B.Sc. in Agriculture', 2),
(3, 'B.Sc. in Allied Health Sciences', 3),
(4, 'Bachelor of Dental Surgery', 4),
(5, 'B.Sc. Engineering', 5),
(6, 'MBBS', 6),
(7, 'B.Sc. in Management', 7),
(8, 'Bachelor of Science', 8),
(9, 'Bachelor of Veterinary Science', 9);

-- ============================================================
-- SEED DATA: District
-- ============================================================
INSERT INTO `district` (`Dist_No`, `District`, `Province`) VALUES
(1, 'Colombo', 'Western'),
(2, 'Gampaha', 'Western'),
(3, 'Kalutara', 'Western'),
(4, 'Kandy', 'Central'),
(5, 'Matale', 'Central'),
(6, 'Nuwara Eliya', 'Central'),
(7, 'Galle', 'Southern'),
(8, 'Matara', 'Southern'),
(9, 'Hambantota', 'Southern'),
(10, 'Jaffna', 'Northern'),
(11, 'Kilinochchi', 'Northern'),
(12, 'Mannar', 'Northern'),
(13, 'Mullaitivu', 'Northern'),
(14, 'Vavuniya', 'Northern'),
(15, 'Batticaloa', 'Eastern'),
(16, 'Ampara', 'Eastern'),
(17, 'Trincomalee', 'Eastern'),
(18, 'Kurunegala', 'North Western'),
(19, 'Puttalam', 'North Western'),
(20, 'Anuradhapura', 'North Central'),
(21, 'Polonnaruwa', 'North Central'),
(22, 'Badulla', 'Uva'),
(23, 'Monaragala', 'Uva'),
(24, 'Ratnapura', 'Sabaragamuwa'),
(25, 'Kegalle', 'Sabaragamuwa');

-- ============================================================
-- SEED DATA: Test Students
-- These are sample records for local development & testing
-- ============================================================

-- Test Student 1: Medicine faculty (M/24/001)
INSERT INTO `stud` (`NIC`, `Reg_No`, `Incno`, `App_Year`, `Faculty`, `Course`, `Status`, `Idrequried`, `Idprinted`, `User`, `Crdate`, `Crtime`) VALUES
('200012345678', 'M/24/001', 1, 2024, 6, 6, 'REGISTERED', 0, 0, 'admin', '2024-01-15', '10:00:00');

INSERT INTO `studbasic` (`NIC`, `Index_No`, `Title`, `L_Name`, `Initials`, `Full_Name`, `Gender`, `ADD1`, `ADD2`, `ADD3`, `Phone_No`, `SelectType`, `RegOn`, `Category`, `Country`, `RegFee`, `FeePaidOn`, `Bank`, `User`, `Crdate`, `Crtime`) VALUES
('200012345678', 100001, 'Mr', 'Perera', 'A.B.', 'Amila Bandara Perera', 'M', 'No 10, Main Street', 'Kandy', 'Central Province', '0771234567', 'MERIT', '2024-02-01', 'LOCAL', 'Sri Lanka', 1000.00, '2024-02-01', 'BOC', 'admin', '2024-01-15', '10:00:00');

INSERT INTO `studother` (`NIC`, `DOB`, `Marital`, `Religion`, `Ethic`, `Nationality`, `Dist_No`, `Z_Score`, `Medium`, `LastSchool`, `Home`, `Mobile`, `Email`, `CAdd1`, `CAdd2`, `CAdd3`, `PName`, `PAdd`, `PTelNo`, `PRelationship`, `EName`, `ETelNo`, `ERelationship`, `Remarks`, `PDist`, `Div_No`, `Police`, `WApp`, `FirstGen`, `User`, `Crdate`, `Crtime`) VALUES
('200012345678', '2000-05-15', 'Single', 'Buddhist', 'Sinhalese', 'Sri Lankan', 4, 1.8500, 'S', 'Royal College Colombo', '0812345678', '0771234567', 'amila.perera@example.com', 'Room 5, Akbar Hall', 'University of Peradeniya', 'Peradeniya', 'Mr. B.C. Perera', 'No 10, Main Street, Kandy', '0812345678', 'Father', 'Dr. D.E. Silva', '0777654321', 'Uncle', NULL, 'Kandy', 1, 'Kandy', '0771234567', 'No', 'admin', '2024-01-15', '10:00:00');

INSERT INTO `studclass` (`Reg_No`, `Semester`, `RegDate`, `User`, `CrDate`, `CrTime`) VALUES
('M/24/001', 3, '2025-06-15', 'admin', '2025-06-15', '09:00:00');

-- Test Student 2: Engineering faculty (E/23/045)
INSERT INTO `stud` (`NIC`, `Reg_No`, `Incno`, `App_Year`, `Faculty`, `Course`, `Status`, `Idrequried`, `Idprinted`, `User`, `Crdate`, `Crtime`) VALUES
('200198765432', 'E/23/045', 45, 2023, 5, 5, 'REGISTERED', 0, 0, 'admin', '2023-08-20', '11:30:00');

INSERT INTO `studbasic` (`NIC`, `Index_No`, `Title`, `L_Name`, `Initials`, `Full_Name`, `Gender`, `ADD1`, `ADD2`, `ADD3`, `Phone_No`, `SelectType`, `RegOn`, `Category`, `Country`, `RegFee`, `FeePaidOn`, `Bank`, `User`, `Crdate`, `Crtime`) VALUES
('200198765432', 100045, 'Ms', 'Fernando', 'C.D.', 'Chathuri Dilhani Fernando', 'F', 'No 25, Temple Road', 'Gampaha', 'Western Province', '0769876543', 'MERIT', '2023-09-01', 'LOCAL', 'Sri Lanka', 1000.00, '2023-09-01', 'NSB', 'admin', '2023-08-20', '11:30:00');

INSERT INTO `studother` (`NIC`, `DOB`, `Marital`, `Religion`, `Ethic`, `Nationality`, `Dist_No`, `Z_Score`, `Medium`, `LastSchool`, `Home`, `Mobile`, `Email`, `CAdd1`, `CAdd2`, `CAdd3`, `PName`, `PAdd`, `PTelNo`, `PRelationship`, `EName`, `ETelNo`, `ERelationship`, `Remarks`, `PDist`, `Div_No`, `Police`, `WApp`, `FirstGen`, `User`, `Crdate`, `Crtime`) VALUES
('200198765432', '2001-03-22', 'Single', 'Catholic', 'Sinhalese', 'Sri Lankan', 2, 2.1200, 'S', 'Visakha Vidyalaya', '0332345678', '0769876543', 'chathuri.fernando@example.com', 'Room 12, Sangamitta Hall', 'University of Peradeniya', 'Peradeniya', 'Mr. D.E. Fernando', 'No 25, Temple Road, Gampaha', '0332345678', 'Father', 'Mrs. F.G. Fernando', '0769876544', 'Mother', NULL, 'Gampaha', 2, 'Gampaha', '0769876543', 'Yes', 'admin', '2023-08-20', '11:30:00');

INSERT INTO `studclass` (`Reg_No`, `Semester`, `RegDate`, `User`, `CrDate`, `CrTime`) VALUES
('E/23/045', 5, '2025-09-01', 'admin', '2025-09-01', '09:00:00');

-- Test Student 3: Science faculty (S/22/100)
INSERT INTO `stud` (`NIC`, `Reg_No`, `Incno`, `App_Year`, `Faculty`, `Course`, `Status`, `Idrequried`, `Idprinted`, `User`, `Crdate`, `Crtime`) VALUES
('199912340000', 'S/22/100', 100, 2022, 8, 8, 'REGISTERED', 0, 0, 'admin', '2022-10-10', '09:00:00');

INSERT INTO `studbasic` (`NIC`, `Index_No`, `Title`, `L_Name`, `Initials`, `Full_Name`, `Gender`, `ADD1`, `ADD2`, `ADD3`, `Phone_No`, `SelectType`, `RegOn`, `Category`, `Country`, `RegFee`, `FeePaidOn`, `Bank`, `User`, `Crdate`, `Crtime`) VALUES
('199912340000', 100100, 'Mr', 'Silva', 'E.F.', 'Eranga Fernando Silva', 'M', 'No 50, Lake Road', 'Matara', 'Southern Province', '0411234567', 'MERIT', '2022-11-01', 'LOCAL', 'Sri Lanka', 1000.00, '2022-11-01', 'PB', 'admin', '2022-10-10', '09:00:00');

INSERT INTO `studother` (`NIC`, `DOB`, `Marital`, `Religion`, `Ethic`, `Nationality`, `Dist_No`, `Z_Score`, `Medium`, `LastSchool`, `Home`, `Mobile`, `Email`, `CAdd1`, `CAdd2`, `CAdd3`, `PName`, `PAdd`, `PTelNo`, `PRelationship`, `EName`, `ETelNo`, `ERelationship`, `Remarks`, `PDist`, `Div_No`, `Police`, `WApp`, `FirstGen`, `User`, `Crdate`, `Crtime`) VALUES
('199912340000', '1999-08-10', 'Single', 'Buddhist', 'Sinhalese', 'Sri Lankan', 8, 1.9800, 'S', 'Rahula College Matara', '0411234567', '0751234567', 'eranga.silva@example.com', 'Room 8, Arunachalam Hall', 'University of Peradeniya', 'Peradeniya', 'Mrs. G.H. Silva', 'No 50, Lake Road, Matara', '0411234567', 'Mother', 'Mr. I.J. Silva', '0751234568', 'Brother', NULL, 'Matara', 3, 'Matara', '0751234567', 'No', 'admin', '2022-10-10', '09:00:00');

INSERT INTO `studclass` (`Reg_No`, `Semester`, `RegDate`, `User`, `CrDate`, `CrTime`) VALUES
('S/22/100', 7, '2025-09-01', 'admin', '2025-09-01', '09:00:00');

SET foreign_key_checks = 1;
