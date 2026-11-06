-- Datos de prueba para la sección de Mantenimientos.
-- Ejecutar después de iniciar el backend una vez, para que existan las tablas.
-- Se puede correr varias veces, no duplica catálogos y reinicia los mantenimientos.

USE montacargas_obregon;

-- Categorías-- Datos de prueba para la sección de Mantenimientos.
-- -- Ejecutar después de iniciar el backend una vez, para que existan las tablas.
-- -- Se puede correr varias veces, no duplica catálogos y reinicia los mantenimientos.
--
-- USE montacargas_obregon;
--
-- -- Categorías
-- INSERT IGNORE INTO categorias (nombre) VALUES
--   ('Montacargas'), ('Retroexcavadora'), ('Generador'), ('Compactador');
--
-- -- Modelos
-- INSERT IGNORE INTO modelos (codigo, nombre, marca, descripcion, tarifa_dia, categoria_id)
-- SELECT d.codigo, d.nombre, d.marca, NULL, d.tarifa, c.id
-- FROM (
--          SELECT '8FGU25' AS codigo, '8FGU25' AS nombre, 'Toyota' AS marca, 1800.00 AS tarifa, 'Montacargas' AS categoria
--          UNION ALL SELECT '8FGU30', '8FGU30', 'Toyota',  2000.00, 'Montacargas'
--          UNION ALL SELECT 'H50FT',  'H50FT',  'Hyster',  1900.00, 'Montacargas'
--          UNION ALL SELECT '1F2',    '1F2',    'Nissan',  1700.00, 'Montacargas'
--          UNION ALL SELECT '420F',   '420F',   'CAT',     3500.00, 'Retroexcavadora'
--          UNION ALL SELECT 'C100D5', 'C100D5', 'Cummins', 2500.00, 'Generador'
--          UNION ALL SELECT 'CA25',   'CA25',   'Dynapac', 2800.00, 'Compactador'
--      ) d
--          JOIN categorias c ON c.nombre = d.categoria;
--
-- -- Equipos
-- INSERT IGNORE INTO equipos (numero_unidad, numero_serie, anio_compra, estado, modelo_id)
-- SELECT d.unidad, d.serie, d.anio, 'OPERATIVO', m.id
-- FROM (
--          SELECT 'MC-014' AS unidad, 'TY25-000014' AS serie, 2021 AS anio, '8FGU25' AS modelo
--          UNION ALL SELECT 'MC-021', 'HY50-000021',   2020, 'H50FT'
--          UNION ALL SELECT 'RT-007', 'CAT420-000007', 2019, '420F'
--          UNION ALL SELECT 'GE-003', 'CU100-000003',  2022, 'C100D5'
--          UNION ALL SELECT 'MC-009', 'NS1F2-000009',  2018, '1F2'
--          UNION ALL SELECT 'CM-002', 'DY25-000002',   2021, 'CA25'
--          UNION ALL SELECT 'MC-017', 'TY30-000017',   2023, '8FGU30'
--      ) d
--          JOIN modelos m ON m.codigo = d.modelo;
--
-- -- Personal: cuatro mecánicos y un operador, para probar que no se le puede asignar
-- INSERT IGNORE INTO personal (codigo, nombres, apellido_paterno, apellido_materno, tipo, activo) VALUES
--   ('MEC-10', 'Rafael',  'Amaya',      NULL, 'MECANICO',    1),
--   ('MEC-11', 'Martín',  'Cota',       NULL, 'MECANICO',    1),
--   ('MEC-12', 'Jorge',   'Ibarra',     NULL, 'MECANICO',    1),
--   ('MEC-13', 'Ernesto', 'Pérez',      NULL, 'MECANICO',    1),
--   ('OPT-10', 'Luis',    'Valenzuela', NULL, 'OP_TRASLADO', 1);
--
-- -- Mantenimientos: se borran y se vuelven a crear
-- DELETE FROM mantenimientos;
--
-- SET @rafael  = (SELECT id FROM personal WHERE codigo = 'MEC-10');
-- SET @martin  = (SELECT id FROM personal WHERE codigo = 'MEC-11');
-- SET @jorge   = (SELECT id FROM personal WHERE codigo = 'MEC-12');
-- SET @ernesto = (SELECT id FROM personal WHERE codigo = 'MEC-13');
--
-- SET @mc014 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-014');
-- SET @mc021 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-021');
-- SET @rt007 = (SELECT id FROM equipos WHERE numero_unidad = 'RT-007');
-- SET @ge003 = (SELECT id FROM equipos WHERE numero_unidad = 'GE-003');
-- SET @mc009 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-009');
-- SET @cm002 = (SELECT id FROM equipos WHERE numero_unidad = 'CM-002');
-- SET @mc017 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-017');
--
-- -- Las fechas son relativas a hoy, para que la demo siempre se vea vigente
-- INSERT INTO mantenimientos
-- (equipo_id, tipo, descripcion_falla, reportado_por_id, fecha_reporte, fecha_limite,
--  mecanico_id, fecha_asignacion, fecha_inicio, fecha_fin, estado)
-- VALUES
--     -- Sin asignar
--     (@mc014, 'CORRECTIVO', 'Fuga de aceite hidráulico', @ernesto,
--      TIMESTAMP(CURDATE(), '09:12:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '08:00:00'),
--      NULL, NULL, NULL, NULL, 'PENDIENTE'),
--     (@mc021, 'CORRECTIVO', 'Frenos con respuesta lenta', @ernesto,
--      TIMESTAMP(CURDATE(), '09:12:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '10:00:00'),
--      NULL, NULL, NULL, NULL, 'PENDIENTE'),
--     (@rt007, 'CORRECTIVO', 'Mangueras de pluma desgastadas', @ernesto,
--      TIMESTAMP(CURDATE(), '09:40:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '08:00:00'),
--      NULL, NULL, NULL, NULL, 'PENDIENTE'),
--     (@ge003, 'CORRECTIVO', 'Batería sin carga', @ernesto,
--      TIMESTAMP(CURDATE(), '10:05:00'), TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '09:00:00'),
--      NULL, NULL, NULL, NULL, 'PENDIENTE'),
--
--     -- Asignado, aún no empieza
--     (@mc009, 'CORRECTIVO', 'Llanta delantera dañada', @ernesto,
--      TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:20:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '14:00:00'),
--      @martin, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '17:00:00'), NULL, NULL, 'ASIGNADO'),
--
--     -- En proceso: ejemplo de registro previo
--     (@cm002, 'CORRECTIVO', 'Luz de operación intermitente', @ernesto,
--      TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '11:05:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '11:00:00'),
--      @jorge, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '12:00:00'),
--      TIMESTAMP(CURDATE(), '08:30:00'), NULL, 'EN_PROCESO'),
--
--     -- Terminado
--     (@mc017, 'CORRECTIVO', 'Claxon sin funcionar', @ernesto,
--      TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '09:30:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '16:00:00'),
--      @jorge, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '10:00:00'),
--      TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '11:00:00'),
--      TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '15:00:00'), 'COMPLETADO');
INSERT IGNORE INTO categorias (nombre) VALUES
  ('Montacargas'), ('Retroexcavadora'), ('Generador'), ('Compactador');

-- Modelos
INSERT IGNORE INTO modelos (codigo, nombre, marca, descripcion, tarifa_dia, categoria_id)
SELECT d.codigo, d.nombre, d.marca, NULL, d.tarifa, c.id
FROM (
         SELECT '8FGU25' AS codigo, '8FGU25' AS nombre, 'Toyota' AS marca, 1800.00 AS tarifa, 'Montacargas' AS categoria
         UNION ALL SELECT '8FGU30', '8FGU30', 'Toyota',  2000.00, 'Montacargas'
         UNION ALL SELECT 'H50FT',  'H50FT',  'Hyster',  1900.00, 'Montacargas'
         UNION ALL SELECT '1F2',    '1F2',    'Nissan',  1700.00, 'Montacargas'
         UNION ALL SELECT '420F',   '420F',   'CAT',     3500.00, 'Retroexcavadora'
         UNION ALL SELECT 'C100D5', 'C100D5', 'Cummins', 2500.00, 'Generador'
         UNION ALL SELECT 'CA25',   'CA25',   'Dynapac', 2800.00, 'Compactador'
     ) d
         JOIN categorias c ON c.nombre = d.categoria;

-- Equipos
INSERT IGNORE INTO equipos (numero_unidad, numero_serie, anio_compra, estado, modelo_id)
SELECT d.unidad, d.serie, d.anio, 'OPERATIVO', m.id
FROM (
         SELECT 'MC-014' AS unidad, 'TY25-000014' AS serie, 2021 AS anio, '8FGU25' AS modelo
         UNION ALL SELECT 'MC-021', 'HY50-000021',   2020, 'H50FT'
         UNION ALL SELECT 'RT-007', 'CAT420-000007', 2019, '420F'
         UNION ALL SELECT 'GE-003', 'CU100-000003',  2022, 'C100D5'
         UNION ALL SELECT 'MC-009', 'NS1F2-000009',  2018, '1F2'
         UNION ALL SELECT 'CM-002', 'DY25-000002',   2021, 'CA25'
         UNION ALL SELECT 'MC-017', 'TY30-000017',   2023, '8FGU30'
     ) d
         JOIN modelos m ON m.codigo = d.modelo;

-- Personal: cuatro mecánicos y un operador, para probar que no se le puede asignar
INSERT IGNORE INTO personal (codigo, nombres, apellido_paterno, apellido_materno, tipo, activo) VALUES
  ('MEC-10', 'Rafael',  'Amaya',      NULL, 'MECANICO',    1),
  ('MEC-11', 'Martín',  'Cota',       NULL, 'MECANICO',    1),
  ('MEC-12', 'Jorge',   'Ibarra',     NULL, 'MECANICO',    1),
  ('MEC-13', 'Ernesto', 'Pérez',      NULL, 'MECANICO',    1),
  ('OPT-10', 'Luis',    'Valenzuela', NULL, 'OP_TRASLADO', 1);

-- Mantenimientos: se borran y se vuelven a crear
DELETE FROM mantenimientos;

SET @rafael  = (SELECT id FROM personal WHERE codigo = 'MEC-10');
SET @martin  = (SELECT id FROM personal WHERE codigo = 'MEC-11');
SET @jorge   = (SELECT id FROM personal WHERE codigo = 'MEC-12');
SET @ernesto = (SELECT id FROM personal WHERE codigo = 'MEC-13');

SET @mc014 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-014');
SET @mc021 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-021');
SET @rt007 = (SELECT id FROM equipos WHERE numero_unidad = 'RT-007');
SET @ge003 = (SELECT id FROM equipos WHERE numero_unidad = 'GE-003');
SET @mc009 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-009');
SET @cm002 = (SELECT id FROM equipos WHERE numero_unidad = 'CM-002');
SET @mc017 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-017');

-- Las fechas son relativas a hoy, para que la demo siempre se vea vigente
INSERT INTO mantenimientos
(equipo_id, tipo, descripcion_falla, reportado_por_id, fecha_reporte, fecha_limite,
 mecanico_id, fecha_asignacion, fecha_inicio, fecha_fin, estado)
VALUES
    -- Sin asignar
    (@mc014, 'CORRECTIVO', 'Fuga de aceite hidráulico', @ernesto,
     TIMESTAMP(CURDATE(), '09:12:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '08:00:00'),
     NULL, NULL, NULL, NULL, 'PENDIENTE'),
    (@mc021, 'CORRECTIVO', 'Frenos con respuesta lenta', @ernesto,
     TIMESTAMP(CURDATE(), '09:12:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '10:00:00'),
     NULL, NULL, NULL, NULL, 'PENDIENTE'),
    (@rt007, 'CORRECTIVO', 'Mangueras de pluma desgastadas', @ernesto,
     TIMESTAMP(CURDATE(), '09:40:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '08:00:00'),
     NULL, NULL, NULL, NULL, 'PENDIENTE'),
    (@ge003, 'CORRECTIVO', 'Batería sin carga', @ernesto,
     TIMESTAMP(CURDATE(), '10:05:00'), TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '09:00:00'),
     NULL, NULL, NULL, NULL, 'PENDIENTE'),

    -- Asignado, aún no empieza
    (@mc009, 'CORRECTIVO', 'Llanta delantera dañada', @ernesto,
     TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '16:20:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '14:00:00'),
     @martin, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '17:00:00'), NULL, NULL, 'ASIGNADO'),

    -- En proceso: ejemplo de registro previo
    (@cm002, 'CORRECTIVO', 'Luz de operación intermitente', @ernesto,
     TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '11:05:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '11:00:00'),
     @jorge, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '12:00:00'),
     TIMESTAMP(CURDATE(), '08:30:00'), NULL, 'EN_PROCESO'),

    -- Terminado
    (@mc017, 'CORRECTIVO', 'Claxon sin funcionar', @ernesto,
     TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '09:30:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '16:00:00'),
     @jorge, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '10:00:00'),
     TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '11:00:00'),
     TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '15:00:00'), 'COMPLETADO');