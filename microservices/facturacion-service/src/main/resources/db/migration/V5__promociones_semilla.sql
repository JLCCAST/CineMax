-- Tipos de beneficio y las promociones de partida.

INSERT INTO promotion_type (name_promotion_type) VALUES
    ('PORCENTAJE'),
    ('MONTO_FIJO'),
    ('DOS_POR_UNO'),
    ('ENTRADA_GRATIS');

INSERT INTO promotion (
    id_promotion_type, name_promotion, promotion_code,
    discount_percentage, discount_fixed_amount, max_discount_amount,
    minimum_purchase_amount, minimum_ticket_quantity,
    start_date, end_date, day_of_week, start_time_from, start_time_to,
    exclude_premiere_movies, required_snack_category_id, customer_condition,
    max_uses_per_user, applies_to, priority, status, created_at)
SELECT t.id_promotion_type, p.nombre, p.codigo,
       p.porcentaje, p.monto_fijo, p.tope,
       p.compra_minima, p.entradas_minimas,
       DATE '2025-07-01', DATE '2026-12-31', p.dias, NULL, p.hasta,
       p.excluye_estrenos, p.categoria, p.condicion,
       p.usos, p.alcance, 0, 'Activo', CURRENT_TIMESTAMP
FROM (VALUES
    -- nombre, tipo, código, %, monto fijo, tope, compra mínima, entradas mínimas,
    -- días, hasta (hora), excluye estrenos, categoría requerida, condición, usos, alcance
    ('Miércoles de cine', 'PORCENTAJE', NULL,     50.00, NULL, NULL,  0.00, 1, '3', NULL::TIME,  TRUE,  NULL::INTEGER, NULL,         NULL::INTEGER, 'ENTRADAS'),
    ('Martes 2x1',        'DOS_POR_UNO', NULL,     NULL, NULL, NULL,  0.00, 2, '2', NULL,        FALSE, NULL,          NULL,         NULL,          'ENTRADAS'),
    ('Matiné',            'PORCENTAJE', NULL,     20.00, NULL, NULL,  0.00, 1, NULL, TIME '15:00', FALSE, NULL,         NULL,         NULL,          'ENTRADAS'),
    -- La categoría 2 es "Combos" en el seed de confiteria_db.
    ('Combo + película',  'MONTO_FIJO', NULL,     NULL, 5.00, NULL,  0.00, 2, NULL, NULL,        FALSE, 2,             NULL,         NULL,          'CONFITERIA'),
    ('Cumpleañero',       'ENTRADA_GRATIS', NULL, NULL, NULL, 25.00, 0.00, 1, NULL, NULL,        FALSE, NULL,          'CUMPLEANOS', 1,             'ENTRADAS'),
    ('Cupón de la app',   'PORCENTAJE', 'APP10',  10.00, NULL, 15.00, 40.00, 1, NULL, NULL,       FALSE, NULL,          NULL,         NULL,          'TODO')
) AS p(nombre, tipo, codigo, porcentaje, monto_fijo, tope, compra_minima, entradas_minimas,
       dias, hasta, excluye_estrenos, categoria, condicion, usos, alcance)
JOIN promotion_type t ON t.name_promotion_type = p.tipo;
