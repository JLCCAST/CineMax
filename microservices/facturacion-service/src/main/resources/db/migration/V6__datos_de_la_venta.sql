-- La venta guarda lo que el cliente compró tal como era en ese momento: película,
-- función, sala y sede, el código de cada butaca y el nombre de cada snack.
-- Así "mis compras" no depende de otros servicios, y una función borrada o un
-- snack renombrado después no cambian el historial.

ALTER TABLE sale_transaction
    ADD COLUMN id_showtime VARCHAR(24),
    ADD COLUMN id_venue    INTEGER,
    ADD COLUMN movie_title VARCHAR(150),
    ADD COLUMN show_date   DATE,
    ADD COLUMN start_time  TIME,
    ADD COLUMN room_name   VARCHAR(100),
    ADD COLUMN venue_name  VARCHAR(150);

COMMENT ON COLUMN sale_transaction.id_showtime IS 'Función comprada; NULL en un pedido solo de confitería';
COMMENT ON COLUMN sale_transaction.id_venue    IS 'Sede de la función o de recojo de la confitería';

ALTER TABLE sale_ticket_detail ADD COLUMN seat_code VARCHAR(10);
ALTER TABLE sale_snack_detail  ADD COLUMN snack_name VARCHAR(100);
