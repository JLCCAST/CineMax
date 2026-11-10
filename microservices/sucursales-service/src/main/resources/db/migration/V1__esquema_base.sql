-- Esquema base de sucursales_db (MySQL).
--
-- Reproduce el esquema que creaba el init-script antes de Flyway, tal como lo
-- muestra SHOW CREATE TABLE. Los nombres district_ibfk_1 y province_ibfk_1 y los
-- índices id_province e id_department son los que MySQL generó entonces.
--
-- No edites este archivo: un cambio de esquema va en una migración nueva (V3, V4...).

CREATE TABLE `department` (
  `id_department` int NOT NULL AUTO_INCREMENT,
  `name_department` varchar(100) NOT NULL,
  PRIMARY KEY (`id_department`),
  UNIQUE KEY `name_department` (`name_department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `province` (
  `id_province` int NOT NULL AUTO_INCREMENT,
  `name_province` varchar(100) NOT NULL,
  `id_department` int NOT NULL,
  PRIMARY KEY (`id_province`),
  KEY `id_department` (`id_department`),
  CONSTRAINT `province_ibfk_1` FOREIGN KEY (`id_department`) REFERENCES `department` (`id_department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `district` (
  `id_district` int NOT NULL AUTO_INCREMENT,
  `name_district` varchar(100) NOT NULL,
  `id_province` int NOT NULL,
  PRIMARY KEY (`id_district`),
  KEY `id_province` (`id_province`),
  CONSTRAINT `district_ibfk_1` FOREIGN KEY (`id_province`) REFERENCES `province` (`id_province`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `seat_type` (
  `id_seat_type` int NOT NULL AUTO_INCREMENT,
  `name_seat_type` varchar(50) NOT NULL,
  PRIMARY KEY (`id_seat_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `venue` (
  `id_venue` int NOT NULL AUTO_INCREMENT,
  `name_venue` varchar(100) NOT NULL,
  `address` varchar(150) NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'Activo',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `image_url` varchar(500) DEFAULT NULL,
  `id_district` int NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  PRIMARY KEY (`id_venue`),
  KEY `fk_venue_district` (`id_district`),
  CONSTRAINT `fk_venue_district` FOREIGN KEY (`id_district`) REFERENCES `district` (`id_district`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `room` (
  `id_room` int NOT NULL AUTO_INCREMENT,
  `name_room` varchar(50) NOT NULL,
  `capacity` int NOT NULL,
  `num_rows` int DEFAULT NULL,
  `seats_per_row` int DEFAULT NULL,
  `status` varchar(20) DEFAULT 'Activo',
  `id_venue` int NOT NULL,
  PRIMARY KEY (`id_room`),
  KEY `fk_room_venue` (`id_venue`),
  CONSTRAINT `fk_room_venue` FOREIGN KEY (`id_venue`) REFERENCES `venue` (`id_venue`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `seat` (
  `id_seat` int NOT NULL AUTO_INCREMENT,
  `row_name` varchar(5) NOT NULL,
  `column_number` int NOT NULL,
  `status` varchar(20) DEFAULT 'ACTIVO',
  `id_seat_type` int NOT NULL,
  `id_room` int NOT NULL,
  PRIMARY KEY (`id_seat`),
  KEY `fk_seat_type` (`id_seat_type`),
  KEY `fk_seat_room` (`id_room`),
  CONSTRAINT `fk_seat_room` FOREIGN KEY (`id_room`) REFERENCES `room` (`id_room`),
  CONSTRAINT `fk_seat_type` FOREIGN KEY (`id_seat_type`) REFERENCES `seat_type` (`id_seat_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
