-- Datos semilla de sucursales_db: ubicaciones, tipos de butaca, sedes, salas y
-- butacas. Son las mismas sentencias del init-script, así que los ids quedan
-- igual (los de las butacas tienen saltos: MySQL reserva ids de más en cada
-- INSERT ... SELECT).
--
-- Diferencia con el init-script: Flyway envía este archivo en UTF-8, así que
-- los textos con tilde quedan bien guardados. El init-script lo cargaba un
-- cliente en latin1 y "María" terminaba guardado como "MarÃ­a".

-- ============================================
-- SEED DATA: Departamentos
-- ============================================
INSERT INTO department (name_department) VALUES
    ('Lima'),
    ('Arequipa'),
    ('Cusco');

-- ============================================
-- SEED DATA: Provincias
-- ============================================
INSERT INTO province (name_province, id_department) VALUES
    ('Lima', 1),
    ('Arequipa', 2),
    ('Cusco', 3);

-- ============================================
-- SEED DATA: Distritos
-- ============================================
INSERT INTO district (name_district, id_province) VALUES
    ('Villa María del Triunfo', 1),
    ('Miraflores', 1),
    ('San Miguel', 1),
    ('Santiago de Surco', 1),
    ('Arequipa (Cercado)', 2),
    ('Yanahuara', 2),
    ('Cusco (Wanchaq)', 3),
    ('Urubamba', 3);

-- ============================================
-- SEED DATA: Tipos de asiento
-- ============================================
INSERT INTO seat_type (name_seat_type) VALUES
    ('REGULAR'),
    ('WHEELCHAIR');

-- ============================================
-- SEED DATA: Sedes (Venues)
-- ============================================
INSERT INTO venue (name_venue, address, phone_number, status, id_district, latitude, longitude) VALUES
    ('CineMax Villa María del Triunfo', 'Av. Bolívar 1234, Villa María del Triunfo', '(01) 555-0101', 'Activo', 1, -12.1542, -76.9383),
    ('CineMax Miraflores', 'Av. Larco 567, Miraflores', '(01) 555-0202', 'Activo', 2, -12.1200, -77.0300);

-- ============================================
-- SEED DATA: Salas (Rooms) para cada sede
-- ============================================
INSERT INTO room (name_room, capacity, num_rows, seats_per_row, status, id_venue) VALUES
    ('Sala 1', 80, 8, 10, 'Activo', 1),
    ('Sala 2', 60, 6, 10, 'Activo', 1),
    ('Sala 3', 40, 5, 8, 'Activo', 1),
    ('Sala 1', 100, 10, 10, 'Activo', 2),
    ('Sala 2', 70, 7, 10, 'Activo', 2),
    ('Sala 3 VIP', 30, 3, 10, 'Activo', 2);

-- ============================================
-- SEED DATA: Asientos (Seats) generados dinámicamente
-- ============================================
-- Genera asientos para cada sala: cada fila (A-Z) × cada columna (1-N)
-- Sala 1 (id=1, VMT): 8 filas (A-H) × 10 columnas = 80 asientos REGULAR
INSERT INTO seat (row_name, column_number, status, id_seat_type, id_room)
SELECT CHAR(64 + r.n), c.n, 'ACTIVO', 1, 1
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) r
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10
) c;

-- Sala 2 (id=2, VMT): 6 filas (A-F) × 10 columnas = 60 asientos REGULAR
INSERT INTO seat (row_name, column_number, status, id_seat_type, id_room)
SELECT CHAR(64 + r.n), c.n, 'ACTIVO', 1, 2
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) r
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10
) c;

-- Sala 3 (id=3, VMT): 5 filas (A-E) × 8 columnas = 40 asientos REGULAR
INSERT INTO seat (row_name, column_number, status, id_seat_type, id_room)
SELECT CHAR(64 + r.n), c.n, 'ACTIVO', 1, 3
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5
) r
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
    UNION ALL SELECT 7 UNION ALL SELECT 8
) c;

-- Sala 4 (id=4, Miraflores): 10 filas (A-J) × 10 columnas = 100 asientos REGULAR
INSERT INTO seat (row_name, column_number, status, id_seat_type, id_room)
SELECT CHAR(64 + r.n), c.n, 'ACTIVO', 1, 4
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10
) r
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10
) c;

-- Sala 5 (id=5, Miraflores): 7 filas (A-G) × 10 columnas = 70 asientos REGULAR
INSERT INTO seat (row_name, column_number, status, id_seat_type, id_room)
SELECT CHAR(64 + r.n), c.n, 'ACTIVO', 1, 5
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
    UNION ALL SELECT 7
) r
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10
) c;

-- Sala 6 (id=6, Miraflores VIP): 3 filas (A-C) × 10 columnas = 30 asientos REGULAR
INSERT INTO seat (row_name, column_number, status, id_seat_type, id_room)
SELECT CHAR(64 + r.n), c.n, 'ACTIVO', 1, 6
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3
) r
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10
) c;
