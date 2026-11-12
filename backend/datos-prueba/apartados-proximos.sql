-- Datos de prueba para los avisos de apartado próximo (OHU-24).
-- Requiere: arrancar el backend una vez (crea la tabla cartas_responsivas)
-- y correr antes mantenimientos.sql y revisiones.sql (crean equipos, clientes y rentas).
-- Se puede correr varias veces: borra y vuelve a crear las cartas.
--
-- Qué se debe ver con 7 días de anticipación:
--   Sin carta:        R-2070 (mañana), R-2071 (en 3 días), R-2083 (en 5 días)
--   Falta la firma:   R-2061 (carta generada, en 2 días), R-2080 (carta enviada, en 6 días)
--   No aparecen:      R-2081 (carta firmada), R-2090 (empieza en 9 días: sale con 10 días),
--                     R-2052 (ya está en curso)

USE montacargas_obregon;

-- Rentas extra, con fechas relativas a hoy
INSERT IGNORE INTO rentas
  (folio, cliente_id, fecha_inicio, fecha_fin, entrega_desde, entrega_hasta,
   recoleccion_desde, recoleccion_hasta, direccion_obra, contacto_obra, telefono_contacto, estado)
SELECT d.folio, c.id, d.inicio, d.fin, d.ent_desde, d.ent_hasta,
       NULL, NULL, d.direccion, d.contacto, d.tel, 'PROGRAMADA'
FROM (
         SELECT 'R-2080' AS folio, 'compras@industriascajeme.mx' AS correo,
                CURDATE() + INTERVAL 6 DAY AS inicio, CURDATE() + INTERVAL 20 DAY AS fin,
                TIMESTAMP(CURDATE() + INTERVAL 6 DAY, '08:00:00') AS ent_desde,
                TIMESTAMP(CURDATE() + INTERVAL 6 DAY, '09:30:00') AS ent_hasta,
                'Parque Industrial Cajeme, Nave 4' AS direccion,
                'Ramón Gutiérrez' AS contacto, '6441000001' AS tel
         UNION ALL SELECT 'R-2081', 'obra@constructorayaqui.mx',
             CURDATE() + INTERVAL 4 DAY, CURDATE() + INTERVAL 12 DAY,
             TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '10:00:00'), TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '12:00:00'),
             'Calle 300 s/n, Valle del Yaqui, Cajeme', 'Laura Méndez', '6441000002'
         UNION ALL SELECT 'R-2083', 'ana@agricolavalle.mx',
             CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 35 DAY,
             NULL, NULL,
             'Carretera a Esperanza km 6, Cajeme', 'Ana Castro', '6441000004'
         UNION ALL SELECT 'R-2090', 'almacen@bodegasnainari.mx',
             CURDATE() + INTERVAL 9 DAY, CURDATE() + INTERVAL 16 DAY,
             TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '13:00:00'), TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '14:30:00'),
             'Av. Náinari 845, Col. Centro, Cd. Obregón', 'Jesús Rivera', '6441000003'
     ) d
         JOIN clientes c ON c.correo = d.correo;

-- Equipos de las rentas extra
INSERT IGNORE INTO equipos_renta (renta_id, equipo_id, fase)
SELECT r.id, e.id, 'RESERVADO'
FROM (
         SELECT 'R-2080' AS folio, 'MC-030' AS unidad
         UNION ALL SELECT 'R-2080', 'MC-031'
         UNION ALL SELECT 'R-2080', 'MC-032'
         UNION ALL SELECT 'R-2081', 'MC-034'
         UNION ALL SELECT 'R-2083', 'MC-033'
         UNION ALL SELECT 'R-2090', 'MC-035'
         UNION ALL SELECT 'R-2090', 'GE-005'
     ) d
         JOIN rentas r ON r.folio = d.folio
         JOIN equipos e ON e.numero_unidad = d.unidad;

-- Cartas: las rentas que no aparecen aquí quedan "sin generar"
DELETE FROM cartas_responsivas;

-- version empieza en 1 al generar la carta (OHU-04)
INSERT INTO cartas_responsivas (renta_id, estado, fecha_generacion, version)
SELECT r.id, d.estado, NOW(), 1
FROM (
         SELECT 'R-2061' AS folio, 'GENERADA' AS estado
         UNION ALL SELECT 'R-2080', 'ENVIADA'
         UNION ALL SELECT 'R-2081', 'FIRMADA'
     ) d
         JOIN rentas r ON r.folio = d.folio;
