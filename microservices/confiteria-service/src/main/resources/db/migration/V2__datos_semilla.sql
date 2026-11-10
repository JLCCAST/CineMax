-- Datos semilla de confiteria_db: categorías, snacks y stock por sede.
-- Son las mismas sentencias del init-script, así que los ids quedan igual.
--
-- Diferencia con el init-script: Flyway envía este archivo en UTF-8, así que
-- los textos con tilde quedan bien guardados. El init-script lo cargaba un
-- cliente en latin1 y "elección" terminaba guardado como "elecciÃ³n".

-- ---- SNACK_CATEGORY ----
INSERT INTO snack_category (name_category) VALUES
('Bebidas'),
('Combos'),
('Dulces'),
('Salados');

-- ---- SNACK ----
-- id_venue usa los mismos venueId que ya tienes en los showtimes de cartelera (1 y 2)
-- para que el frontend muestre cosas consistentes en ambas sucursales.
INSERT INTO snack (id_venue, id_snack_category, name_snack, description_snack, price, stock, image_url_snack, status) VALUES
(1, (SELECT id_snack_category FROM snack_category WHERE name_category = 'Bebidas'),
    'Gaseosa Grande', 'Vaso grande de gaseosa a elección', 8.50, 100, '', 'Activo'),

(1, (SELECT id_snack_category FROM snack_category WHERE name_category = 'Bebidas'),
    'Agua Mineral', 'Botella de agua mineral 500ml', 5.00, 150, '', 'Activo'),

(1, (SELECT id_snack_category FROM snack_category WHERE name_category = 'Combos'),
    'Combo Pareja', 'Canchita grande + 2 gaseosas medianas', 25.00, 40, '', 'Activo'),

(1, (SELECT id_snack_category FROM snack_category WHERE name_category = 'Combos'),
    'Combo Familiar', 'Canchita extra grande + 4 gaseosas + dulces', 45.00, 25, '', 'Activo'),

(1, (SELECT id_snack_category FROM snack_category WHERE name_category = 'Dulces'),
    'Chocolate M&Ms', 'Bolsa mediana de chocolates M&Ms', 9.00, 80, '', 'Activo'),

(1, (SELECT id_snack_category FROM snack_category WHERE name_category = 'Salados'),
    'Canchita Grande', 'Canchita salada tamaño grande', 12.00, 60, '', 'Activo'),

(1, (SELECT id_snack_category FROM snack_category WHERE name_category = 'Salados'),
    'Nachos con Queso', 'Nachos con salsa de queso cheddar', 15.00, 50, '', 'Activo');

-- ---- SNACK_VENUE_STOCK ----
-- Stock por sucursal (venueId 1 y 2) para cada snack recién creado.
INSERT INTO snack_venue_stock (id_snack, id_venue, stock, status)
SELECT id_snack, 1, stock, 'Activo' FROM snack;

INSERT INTO snack_venue_stock (id_snack, id_venue, stock, status)
SELECT id_snack, 2, FLOOR(stock * 0.7), 'Activo' FROM snack;
