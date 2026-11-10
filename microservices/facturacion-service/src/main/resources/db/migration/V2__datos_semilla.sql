-- Datos semilla de sales_db: los estados de una transacción.
-- Son los mismos que cargaba el init-script.
--
-- Nota: la compra busca un estado COMPLETADO que no existe. Agregarlo forma
-- parte del arreglo del flujo de compra y llegará como una migración nueva.

INSERT INTO transaction_status (name_status) VALUES
    ('PENDIENTE'),
    ('PAGADO'),
    ('CANCELADO');
