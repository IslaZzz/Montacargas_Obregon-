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

-- Toda renta sale de una cotización aceptada (HU-37): COT-R2061 es la de R-2061.
-- Se crean solo si no existen. Sus equipos y totales se llenan después, con los de la renta.
SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2080'),
    @entrega = TIMESTAMP(CURDATE() + INTERVAL 6 DAY, '08:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 20 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Parque Industrial Cajeme', 'Nave 4', 'Parque Industrial', 'Cd. Obregón', 'Sonora', '85065' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2080', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Ramón Gutiérrez', '6441000001', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'compras@industriascajeme.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2081'),
    @entrega = TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '10:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Calle 300', 's/n', 'Valle del Yaqui', 'Cajeme', 'Sonora', '85203' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2081', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Laura Méndez', '6441000002', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'obra@constructorayaqui.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2083'),
    @entrega = TIMESTAMP(CURDATE() + INTERVAL 5 DAY, '09:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 35 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Carretera a Esperanza', 'km 6', 'Campo agrícola', 'Cajeme', 'Sonora', '85210' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2083', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Ana Castro', '6441000004', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'ana@agricolavalle.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2090'),
    @entrega = TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '13:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 16 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Av. Náinari', '845', 'Centro', 'Cd. Obregón', 'Sonora', '85000' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2090', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Jesús Rivera', '6441000003', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'almacen@bodegasnainari.mx' AND @falta;

-- Rentas extra, con fechas relativas a hoy
INSERT IGNORE INTO rentas
  (folio, cliente_id, cotizacion_id, fecha_inicio, fecha_fin, entrega_desde, entrega_hasta,
   recoleccion_desde, recoleccion_hasta, direccion_obra, contacto_obra, telefono_contacto, estado)
SELECT d.folio, c.id, q.id, d.inicio, d.fin, d.ent_desde, d.ent_hasta,
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
         JOIN clientes c ON c.correo = d.correo
         JOIN cotizaciones q ON q.folio = CONCAT('COT-', REPLACE(d.folio, '-', ''));

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

-- Cada cotización lleva los equipos de su renta, con la tarifa del modelo; de ahí salen sus totales (IVA 16%)
INSERT INTO equipos_cotizacion (equipo_id, tarifa_dia, importe, cotizacion_id)
SELECT er.equipo_id, m.tarifa_dia, m.tarifa_dia * (DATEDIFF(r.fecha_fin, r.fecha_inicio) + 1), r.cotizacion_id
FROM rentas r
         JOIN equipos_renta er ON er.renta_id = r.id
         JOIN equipos e ON e.id = er.equipo_id
         JOIN modelos m ON m.id = e.modelo_id
WHERE r.folio IN ('R-2080', 'R-2081', 'R-2083', 'R-2090')
  AND NOT EXISTS (SELECT 1 FROM equipos_cotizacion ec
                  WHERE ec.cotizacion_id = r.cotizacion_id AND ec.equipo_id = er.equipo_id);

UPDATE cotizaciones q
    JOIN (SELECT cotizacion_id, SUM(importe) AS subtotal FROM equipos_cotizacion GROUP BY cotizacion_id) t
    ON t.cotizacion_id = q.id
SET q.subtotal = t.subtotal, q.iva = ROUND(t.subtotal * 0.16, 2), q.total = t.subtotal + ROUND(t.subtotal * 0.16, 2)
WHERE q.folio IN ('COT-R2080', 'COT-R2081', 'COT-R2083', 'COT-R2090');

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
