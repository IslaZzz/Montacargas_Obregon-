-- Datos iniciales del sistema (T-02): usuarios de Comercial, categorías, modelos con su tarifa por día
-- y equipos de ejemplo. Son ficticios y las tarifas no incluyen IVA.
-- Requiere: arrancar el backend una vez, para que existan las tablas.
-- Se corre desde la raíz del repositorio (en PowerShell, dentro de cmd /c "..."; ver el README):
--   mysql -u root -p --default-character-set=utf8mb4 < backend/datos-prueba/datos-iniciales.sql
-- o abriéndolo en MySQL Workbench y ejecutándolo.
-- Se puede correr varias veces: solo agrega lo que falte y no borra nada.
--
-- Los equipos son los de la hoja «Datos de prueba» de los casos de Delivery S1.
-- «Disponible» se guarda como OPERATIVO: es el estado de un equipo que se puede rentar.

USE montacargas_obregon;

-- Usuarios: gerencia y auxiliares. Con su correo se inicia sesión (HU-28).
-- Los tres tienen la misma contraseña de prueba; aquí solo va cifrada (BCrypt). La contraseña
-- está en la hoja «Datos de prueba» de los casos de prueba; no se escribe en el repositorio.
SET @contrasena_prueba = '$2a$10$nogcCvdq7InGRTX2APaXeOLEvkltXYK.4CpCCWnZeXhSTUgLSHGQu';

INSERT IGNORE INTO usuarios (numero_empleado, nombres, apellido_paterno, apellido_materno, correo, contrasena, tipo, activo) VALUES
  ('GER-01', 'Patricia', 'Romero', 'Soto',    'patricia.romero@montacargasobregon.mx', @contrasena_prueba, 'GERENTE',  1),
  ('AUX-01', 'Daniela',  'Félix',  'Moreno',  'daniela.felix@montacargasobregon.mx',   @contrasena_prueba, 'AUXILIAR', 1),
  ('AUX-02', 'Luis',     'Ochoa',  'Valdez',  'luis.ochoa@montacargasobregon.mx',      @contrasena_prueba, 'AUXILIAR', 1);

-- Bases de antes de HU-28: el backend agrega la columna vacía; se les pone la contraseña de prueba
UPDATE usuarios SET contrasena = @contrasena_prueba
WHERE (contrasena IS NULL OR contrasena = '')
  AND correo IN ('patricia.romero@montacargasobregon.mx', 'daniela.felix@montacargasobregon.mx',
                 'luis.ochoa@montacargasobregon.mx');

-- Categorías. «Maniobras» no tiene equipos: sirve para probar la búsqueda sin resultados (HU-09)
INSERT IGNORE INTO categorias (nombre) VALUES
  ('Montacargas'), ('Grúa'), ('Patín'), ('Maniobras');

-- Modelos con sus datos y su tarifa por día.
-- Si el modelo ya existía sin descripción (lo crea otro script), se le agrega.
INSERT INTO modelos (codigo, nombre, marca, descripcion, tarifa_dia, categoria_id)
SELECT d.codigo, d.nombre, d.marca, d.descripcion, d.tarifa, c.id
FROM (
         SELECT '8FBE30' AS codigo, 'Montacargas eléctrico 3 t' AS nombre, 'Toyota' AS marca,
                'Eléctrico de 4 ruedas, capacidad de 3,000 kg y mástil triple de 4.7 m. Para interiores.' AS descripcion,
                500.00 AS tarifa, 'Montacargas' AS categoria
         UNION ALL SELECT 'H110FT', 'Montacargas de combustión 5 t', 'Hyster',
                'Diésel, capacidad de 5,000 kg, mástil doble de 3.6 m y llantas neumáticas para exterior.',
                750.00, 'Montacargas'
         UNION ALL SELECT 'X-HIDUO 128', 'Grúa articulada 12 t', 'Hiab',
                'Grúa articulada montada en camión, capacidad de 12 t y alcance hidráulico de 8 m.',
                1200.00, 'Grúa'
         UNION ALL SELECT 'PTH50', 'Patín hidráulico 2.5 t', 'Crown',
                'Manual, capacidad de 2,500 kg y horquillas de 1.15 m.',
                150.00, 'Patín'
     ) d
         JOIN categorias c ON c.nombre = d.categoria
ON DUPLICATE KEY UPDATE descripcion = COALESCE(modelos.descripcion, d.descripcion);

-- Equipos: 5 disponibles y MC-15 en mantenimiento, que no debe aparecer en la disponibilidad (TC-09-05)
INSERT IGNORE INTO equipos (numero_unidad, numero_serie, anio_compra, estado, modelo_id)
SELECT d.unidad, d.serie, d.anio, d.estado, m.id
FROM (
         SELECT 'MC-07' AS unidad, 'TY8FBE-000007' AS serie, 2022 AS anio, 'OPERATIVO' AS estado, '8FBE30' AS modelo
         UNION ALL SELECT 'MC-11', 'TY8FBE-000011',  2023, 'OPERATIVO',        '8FBE30'
         UNION ALL SELECT 'MC-15', 'HYH110-000015',  2020, 'EN_MANTENIMIENTO', 'H110FT'
         UNION ALL SELECT 'MC-16', 'HYH110-000016',  2021, 'OPERATIVO',        'H110FT'
         UNION ALL SELECT 'GR-02', 'HIAB128-000002', 2021, 'OPERATIVO',        'X-HIDUO 128'
         UNION ALL SELECT 'PT-03', 'CRPTH50-000003', 2023, 'OPERATIVO',        'PTH50'
     ) d
         JOIN modelos m ON m.codigo = d.modelo;

-- Etiquetas: solo MC-11 lleva «Uso en interiores»; MC-07 va sin etiqueta (TC-09-06)
INSERT IGNORE INTO equipo_etiquetas (equipo_id, etiqueta)
SELECT e.id, 'USO_INTERIORES'
FROM equipos e
WHERE e.numero_unidad = 'MC-11';


-- Personal operativo: mecánicos y operadores de traslado
INSERT IGNORE INTO personal (codigo, nombres, apellido_paterno, apellido_materno, tipo, activo) VALUES
  ('MEC-10', 'Rafael', 'Amaya',      NULL, 'MECANICO',    1),
  ('MEC-11', 'Martín', 'Cota',       NULL, 'MECANICO',    1),
  ('MEC-12', 'Jorge',  'Ibarra',     NULL, 'MECANICO',    1),
  ('OPT-10', 'Luis',   'Valenzuela', NULL, 'OP_TRASLADO', 1),
  ('OPT-11', 'Ana',    'Soto',       NULL, 'OP_TRASLADO', 1),
  ('OPT-12', 'Ramón',  'Félix',      NULL, 'OP_TRASLADO', 1);
