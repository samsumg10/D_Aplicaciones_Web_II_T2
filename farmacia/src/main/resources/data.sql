-- Catálogo inicial de demostración. Cada INSERT solo se ejecuta si el código aún no existe,
-- así el script puede correr en cada arranque sin duplicar filas.
INSERT INTO medicamento (codigo, nombre, presentacion, stock, fecha_registro)
SELECT 'MED-0001', 'Producto demo A (analgésico)', 'Caja x 100 tabletas', 500, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM medicamento WHERE codigo = 'MED-0001');

INSERT INTO medicamento (codigo, nombre, presentacion, stock, fecha_registro)
SELECT 'MED-0002', 'Producto demo B (antiséptico)', 'Frasco 120 ml', 80, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM medicamento WHERE codigo = 'MED-0002');

INSERT INTO medicamento (codigo, nombre, presentacion, stock, fecha_registro)
SELECT 'MED-0003', 'Producto demo C (suero)', 'Bolsa 1000 ml', 5, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM medicamento WHERE codigo = 'MED-0003');
