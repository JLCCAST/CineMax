-- Texto e imagen con que la portada muestra cada promoción. Los edita el admin.
ALTER TABLE promotion ADD COLUMN description VARCHAR(300);

UPDATE promotion AS p
SET description = d.texto,
    image_url_promotion = COALESCE(p.image_url_promotion, d.imagen)
FROM (VALUES
    ('Miércoles de cine', 'Todos los miércoles, 50 % de descuento en tus entradas. No aplica a estrenos.',
     'Imagenes/Promos/miercoles-de-cine.jpg'),
    ('Martes 2x1', 'Los martes, compra dos entradas y la de menor precio va por nuestra cuenta.',
     'Imagenes/Promos/martes-2x1.jpg'),
    ('Matiné', '20 % de descuento en entradas para funciones que empiezan antes de las 3:00 p. m.',
     'Imagenes/Promos/matine.jpg'),
    ('Combo + película', 'S/ 5 de descuento en tu combo al comprar dos entradas o más.',
     'Imagenes/Promos/combo-pelicula.jpg'),
    ('Cumpleañero', 'En tu cumpleaños, una entrada gratis (hasta S/ 25). Una vez al año.',
     'Imagenes/Promos/cumpleanero.jpg'),
    ('Cupón de la app', 'Usa el código APP10: 10 % en compras desde S/ 40, con un tope de S/ 15.',
     'Imagenes/Promos/cupon-app.jpg')
) AS d(nombre, texto, imagen)
WHERE p.name_promotion = d.nombre;
