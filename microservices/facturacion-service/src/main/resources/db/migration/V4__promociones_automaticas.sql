-- Promociones automáticas: condiciones nuevas y descuento por línea de venta.

ALTER TABLE promotion
    ADD COLUMN applies_to                 VARCHAR(20) NOT NULL DEFAULT 'TODO',
    ADD COLUMN start_time_from            TIME,
    ADD COLUMN start_time_to              TIME,
    ADD COLUMN required_snack_category_id INTEGER,
    ADD COLUMN customer_condition         VARCHAR(20),
    ADD COLUMN max_uses_per_user          INTEGER,
    ADD COLUMN priority                   INTEGER     NOT NULL DEFAULT 0,
    ADD CONSTRAINT promotion_applies_to_check
        CHECK (applies_to IN ('ENTRADAS', 'CONFITERIA', 'TODO'));

COMMENT ON COLUMN promotion.day_of_week IS 'Días ISO separados por coma: 1 = lunes ... 7 = domingo';
COMMENT ON COLUMN promotion.start_time_to IS 'La función debe empezar antes de esta hora';
COMMENT ON COLUMN promotion.required_snack_category_id IS 'Categoría de confiteria_db; sin clave foránea porque vive en otra base';
COMMENT ON COLUMN promotion.max_uses_per_user IS 'Usos por cliente dentro del año calendario';

ALTER TABLE sale_ticket_detail
    ADD COLUMN fare_category   VARCHAR(20),
    ADD COLUMN discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0;

ALTER TABLE sale_snack_detail
    ADD COLUMN discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0;
