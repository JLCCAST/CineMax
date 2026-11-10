-- Esquema base de confiteria_db (MySQL).
--
-- Reproduce el esquema que creaba el init-script antes de Flyway, tal como lo
-- muestra SHOW CREATE TABLE. Los nombres snack_ibfk_1 y snack_venue_stock_ibfk_1
-- y los índices id_snack_category e id_snack son los que MySQL generó entonces.
--
-- No edites este archivo: un cambio de esquema va en una migración nueva (V3, V4...).

CREATE TABLE `snack_category` (
  `id_snack_category` int NOT NULL AUTO_INCREMENT,
  `name_category` varchar(50) NOT NULL,
  PRIMARY KEY (`id_snack_category`),
  UNIQUE KEY `name_category` (`name_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `snack` (
  `id_snack` int NOT NULL AUTO_INCREMENT,
  `id_venue` int DEFAULT NULL,
  `id_snack_category` int NOT NULL,
  `name_snack` varchar(100) NOT NULL,
  `description_snack` varchar(250) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock` int NOT NULL,
  `image_url_snack` varchar(250) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'Activo',
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id_snack`),
  KEY `id_snack_category` (`id_snack_category`),
  CONSTRAINT `snack_ibfk_1` FOREIGN KEY (`id_snack_category`) REFERENCES `snack_category` (`id_snack_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `snack_venue_stock` (
  `id_snack_venue_stock` int NOT NULL AUTO_INCREMENT,
  `id_snack` int NOT NULL,
  `id_venue` int NOT NULL,
  `stock` int NOT NULL,
  `status` varchar(20) DEFAULT 'Activo',
  PRIMARY KEY (`id_snack_venue_stock`),
  KEY `id_snack` (`id_snack`),
  CONSTRAINT `snack_venue_stock_ibfk_1` FOREIGN KEY (`id_snack`) REFERENCES `snack` (`id_snack`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
