-- Las seis promociones de V5 vencían el 2026-12-31. Se extienden un año más.
-- Solo se tocan las que siguen con la fecha original: si alguien ya cambió la
-- vigencia desde el admin, se respeta.
UPDATE promotion
SET end_date = DATE '2027-12-31',
    updated_at = CURRENT_TIMESTAMP
WHERE end_date = DATE '2026-12-31'
  AND name_promotion IN ('Miércoles de cine', 'Martes 2x1', 'Matiné', 'Combo + película',
                         'Cumpleañero', 'Cupón de la app');
