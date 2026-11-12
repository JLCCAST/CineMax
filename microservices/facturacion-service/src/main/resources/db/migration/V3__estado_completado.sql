-- La compra registra cada venta con el estado COMPLETADO.
INSERT INTO transaction_status (name_status) VALUES ('COMPLETADO')
ON CONFLICT (name_status) DO NOTHING;
