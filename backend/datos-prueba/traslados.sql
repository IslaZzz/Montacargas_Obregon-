-- Datos de prueba para la sección de Traslados (HU: Asignar traslados y responsables y app móvil).
-- Requiere: arrancar el backend al menos una vez (crea las tablas) y correr antes
-- datos-iniciales.sql y mantenimientos.sql (crean los equipos usados por las rentas).
-- Se puede correr varias veces: no duplica clientes ni personal y reinicia los traslados.

USE montacargas_obregon;

-- Operadores de traslado
INSERT IGNORE INTO personal (codigo, nombres, apellido_paterno, apellido_materno, tipo, activo) VALUES
  ('OPT-10', 'Luis',    'Valenzuela', NULL, 'OP_TRASLADO', 1),
  ('OPT-11', 'Ana',     'Soto',       NULL, 'OP_TRASLADO', 1),
  ('OPT-12', 'Ramón',   'Félix',      NULL, 'OP_TRASLADO', 1),
  ('OPT-13', 'Héctor',  'Quintero',   NULL, 'OP_TRASLADO', 1);

-- Clientes
INSERT INTO clientes (nombre, apellido_paterno, apellido_materno, empresa, telefono, correo, fecha_registro)
SELECT d.nombre, d.paterno, NULL, d.empresa, d.telefono, d.correo, CURDATE()
FROM (
         SELECT 'Ramón' AS nombre, 'Gutiérrez' AS paterno, 'Industrias Cajeme' AS empresa,
                '6441000001' AS telefono, 'compras@industriascajeme.mx' AS correo
         UNION ALL SELECT 'Laura', 'Méndez', 'Constructora del Yaqui',
                '6441000002', 'obra@constructorayaqui.mx'
         UNION ALL SELECT 'Jesús', 'Rivera', 'Bodegas Náinari',
                '6441000003', 'almacen@bodegasnainari.mx'
         UNION ALL SELECT 'Carlos', 'Vega', 'Empacadora del Noroeste',
                '6441000005', 'contacto@empacadoranoroeste.mx'
         UNION ALL SELECT 'Ana', 'Castro', 'Agrícola Valle del Yaqui',
                '6441000004', 'ana@agricolavalle.mx'
         UNION ALL SELECT 'Juan', 'Pérez', 'Hotel Valle Grande',
                '6441000006', 'reservas@hotelvallegrande.mx'
     ) d
WHERE NOT EXISTS (SELECT 1 FROM clientes c WHERE c.correo = d.correo);

-- Las cotizaciones piden la razón social del cliente: a quien no tenga se le crea una
SET @cliente = (SELECT id FROM clientes WHERE correo = 'compras@industriascajeme.mx' ORDER BY id LIMIT 1);
SET @falta = @cliente IS NOT NULL AND NOT EXISTS (SELECT 1 FROM razones_sociales WHERE cliente_id = @cliente);
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Blvd. Rodolfo Elías Calles', '1520', 'Centro', 'Cd. Obregón', '85000' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Industrias Cajeme S.A. de C.V.', 'ICA150101AB1', LAST_INSERT_ID(), @cliente FROM DUAL WHERE @falta;

SET @cliente = (SELECT id FROM clientes WHERE correo = 'obra@constructorayaqui.mx' ORDER BY id LIMIT 1);
SET @falta = @cliente IS NOT NULL AND NOT EXISTS (SELECT 1 FROM razones_sociales WHERE cliente_id = @cliente);
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Calle 200', 's/n', 'Valle del Yaqui', 'Cajeme', '85203' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Constructora del Yaqui S.A. de C.V.', 'CYQ160202CD2', LAST_INSERT_ID(), @cliente FROM DUAL WHERE @falta;

SET @cliente = (SELECT id FROM clientes WHERE correo = 'almacen@bodegasnainari.mx' ORDER BY id LIMIT 1);
SET @falta = @cliente IS NOT NULL AND NOT EXISTS (SELECT 1 FROM razones_sociales WHERE cliente_id = @cliente);
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Av. Náinari', '845', 'Centro', 'Cd. Obregón', '85000' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Bodegas Náinari S.A. de C.V.', 'BNA170303EF3', LAST_INSERT_ID(), @cliente FROM DUAL WHERE @falta;

SET @cliente = (SELECT id FROM clientes WHERE correo = 'contacto@empacadoranoroeste.mx' ORDER BY id LIMIT 1);
SET @falta = @cliente IS NOT NULL AND NOT EXISTS (SELECT 1 FROM razones_sociales WHERE cliente_id = @cliente);
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Calle Norte', '300', 'Parque Industrial', 'Cd. Obregón', '85065' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Empacadora del Noroeste S.A. de C.V.', 'ENO190505IJ5', LAST_INSERT_ID(), @cliente FROM DUAL WHERE @falta;

SET @cliente = (SELECT id FROM clientes WHERE correo = 'ana@agricolavalle.mx' ORDER BY id LIMIT 1);
SET @falta = @cliente IS NOT NULL AND NOT EXISTS (SELECT 1 FROM razones_sociales WHERE cliente_id = @cliente);
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Carretera a Esperanza', 'km 6', 'Campo agrícola', 'Cajeme', '85210' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Agrícola Valle del Yaqui S.P.R. de R.L.', 'AVY180404GH4', LAST_INSERT_ID(), @cliente FROM DUAL WHERE @falta;

SET @cliente = (SELECT id FROM clientes WHERE correo = 'reservas@hotelvallegrande.mx' ORDER BY id LIMIT 1);
SET @falta = @cliente IS NOT NULL AND NOT EXISTS (SELECT 1 FROM razones_sociales WHERE cliente_id = @cliente);
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Calle Guerrero', '402 Ote.', 'Centro', 'Cd. Obregón', '85000' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Hotel Valle Grande S.A. de C.V.', 'HVG200606KL6', LAST_INSERT_ID(), @cliente FROM DUAL WHERE @falta;

-- Toda renta sale de una cotización aceptada (HU-37). Sus equipos y totales se llenan después, con los de la renta.
-- Fechas relativas a CURDATE() para que la demo y los traslados en campo estén siempre vigentes.
SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2046'),
    @entrega = TIMESTAMP(CURDATE(), '08:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 18 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Blvd. Rodolfo Elías Calles', '1520', 'Centro', 'Cd. Obregón', 'Sonora', '85000' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, precio_definido, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2046', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Ramón Gutiérrez', '6441000001', 0.00, 0.00,
       0.00, 0.00, 0.00, 1, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'compras@industriascajeme.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2048'),
    @entrega = TIMESTAMP(CURDATE(), '10:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '16:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Calle 200', 's/n', 'Valle del Yaqui', 'Cajeme', 'Sonora', '85203' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, precio_definido, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2048', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Laura Méndez', '6441000002', 0.00, 0.00,
       0.00, 0.00, 0.00, 1, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'obra@constructorayaqui.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2050'),
    @entrega = TIMESTAMP(CURDATE(), '13:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 21 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Av. Náinari', '845', 'Centro', 'Cd. Obregón', 'Sonora', '85000' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, precio_definido, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2050', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Jesús Rivera', '6441000003', 0.00, 0.00,
       0.00, 0.00, 0.00, 1, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'almacen@bodegasnainari.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2051'),
    @entrega = TIMESTAMP(CURDATE(), '15:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 15 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Calle Norte', '300', 'Parque Industrial', 'Cd. Obregón', 'Sonora', '85065' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, precio_definido, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2051', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Carlos Vega', '6441000005', 0.00, 0.00,
       0.00, 0.00, 0.00, 1, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'contacto@empacadoranoroeste.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2039'),
    @entrega = TIMESTAMP(CURDATE() - INTERVAL 17 DAY, '09:00:00'), @recoleccion = TIMESTAMP(CURDATE(), '16:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Carretera a Esperanza', 'km 6', 'Campo agrícola', 'Cajeme', 'Sonora', '85210' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, precio_definido, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2039', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Ana Castro', '6441000004', 0.00, 0.00,
       0.00, 0.00, 0.00, 1, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'ana@agricolavalle.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2044'),
    @entrega = TIMESTAMP(CURDATE() - INTERVAL 13 DAY, '10:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '09:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Calle Guerrero', '402 Ote.', 'Centro', 'Cd. Obregón', 'Sonora', '85000' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, precio_definido, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2044', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Juan Pérez', '6441000006', 0.00, 0.00,
       0.00, 0.00, 0.00, 1, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'reservas@hotelvallegrande.mx' AND @falta;

-- Rentas correspondientes a los traslados
INSERT IGNORE INTO rentas
  (folio, cliente_id, cotizacion_id, fecha_inicio, fecha_fin, entrega_desde, entrega_hasta,
   recoleccion_desde, recoleccion_hasta, direccion_obra, contacto_obra, telefono_contacto, estado)
SELECT d.folio, c.id, q.id, d.inicio, d.fin, d.ent_desde, d.ent_hasta,
       d.rec_desde, d.rec_hasta, d.direccion, d.contacto, d.tel, d.estado
FROM (
         SELECT 'R-2046' AS folio, 'compras@industriascajeme.mx' AS correo,
                CURDATE() AS inicio, CURDATE() + INTERVAL 18 DAY AS fin,
                TIMESTAMP(CURDATE(), '08:00:00') AS ent_desde,
                TIMESTAMP(CURDATE(), '09:30:00') AS ent_hasta,
                CAST(NULL AS DATETIME) AS rec_desde, CAST(NULL AS DATETIME) AS rec_hasta,
                'Blvd. Rodolfo Elías Calles 1520, Cd. Obregón' AS direccion,
                'Ramón Gutiérrez' AS contacto, '6441000001' AS tel, 'EN_CURSO' AS estado
         UNION ALL SELECT 'R-2048', 'obra@constructorayaqui.mx',
                CURDATE(), CURDATE() + INTERVAL 12 DAY,
                TIMESTAMP(CURDATE(), '10:00:00'), TIMESTAMP(CURDATE(), '12:00:00'),
                TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '16:00:00'), TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '18:00:00'),
                'Calle 200 s/n, Valle del Yaqui, Cajeme',
                'Laura Méndez', '6441000002', 'PROGRAMADA'
         UNION ALL SELECT 'R-2050', 'almacen@bodegasnainari.mx',
                CURDATE(), CURDATE() + INTERVAL 21 DAY,
                TIMESTAMP(CURDATE(), '13:00:00'), TIMESTAMP(CURDATE(), '14:30:00'),
                CAST(NULL AS DATETIME), CAST(NULL AS DATETIME),
                'Av. Náinari 845, Col. Centro, Cd. Obregón',
                'Jesús Rivera', '6441000003', 'PROGRAMADA'
         UNION ALL SELECT 'R-2051', 'contacto@empacadoranoroeste.mx',
                CURDATE(), CURDATE() + INTERVAL 15 DAY,
                TIMESTAMP(CURDATE(), '15:00:00'), TIMESTAMP(CURDATE(), '17:00:00'),
                CAST(NULL AS DATETIME), CAST(NULL AS DATETIME),
                'Calle Norte 300, Parque Industrial, Cd. Obregón',
                'Carlos Vega', '6441000005', 'PROGRAMADA'
         UNION ALL SELECT 'R-2039', 'ana@agricolavalle.mx',
                CURDATE() - INTERVAL 17 DAY, CURDATE(),
                TIMESTAMP(CURDATE() - INTERVAL 17 DAY, '09:00:00'), TIMESTAMP(CURDATE() - INTERVAL 17 DAY, '11:00:00'),
                TIMESTAMP(CURDATE(), '16:00:00'), TIMESTAMP(CURDATE(), '17:30:00'),
                'Carretera a Esperanza km 6, Cajeme',
                'Ana Castro', '6441000004', 'EN_CURSO'
         UNION ALL SELECT 'R-2044', 'reservas@hotelvallegrande.mx',
                CURDATE() - INTERVAL 13 DAY, CURDATE() + INTERVAL 1 DAY,
                TIMESTAMP(CURDATE() - INTERVAL 13 DAY, '10:00:00'), TIMESTAMP(CURDATE() - INTERVAL 13 DAY, '12:00:00'),
                TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '09:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '11:00:00'),
                'Calle Guerrero 402 Ote., Cd. Obregón',
                'Juan Pérez', '6441000006', 'EN_CURSO'
     ) d
         JOIN clientes c ON c.correo = d.correo
         JOIN cotizaciones q ON q.folio = CONCAT('COT-', REPLACE(d.folio, '-', ''));

-- Equipos de cada renta (creados previamente por datos-iniciales.sql y mantenimientos.sql)
INSERT IGNORE INTO equipos_renta (renta_id, equipo_id, fase)
SELECT r.id, e.id, d.fase
FROM (
         SELECT 'R-2046' AS folio, 'MC-16'  AS unidad, 'EN_TRASLADO'   AS fase
         UNION ALL SELECT 'R-2048', 'MC-11',  'LISTO_ENTREGA'
         UNION ALL SELECT 'R-2050', 'MC-017', 'LISTO_ENTREGA'
         UNION ALL SELECT 'R-2051', 'MC-009', 'LISTO_ENTREGA'
         UNION ALL SELECT 'R-2039', 'MC-021', 'EN_OPERACION'
         UNION ALL SELECT 'R-2044', 'RT-007', 'EN_OPERACION'
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
WHERE r.folio IN ('R-2046', 'R-2048', 'R-2050', 'R-2051', 'R-2039', 'R-2044')
  AND NOT EXISTS (SELECT 1 FROM equipos_cotizacion ec
                  WHERE ec.cotizacion_id = r.cotizacion_id AND ec.equipo_id = er.equipo_id);

UPDATE cotizaciones q
    JOIN (SELECT cotizacion_id, SUM(importe) AS subtotal FROM equipos_cotizacion GROUP BY cotizacion_id) t
    ON t.cotizacion_id = q.id
SET q.subtotal = t.subtotal, q.iva = ROUND(t.subtotal * 0.16, 2), q.total = t.subtotal + ROUND(t.subtotal * 0.16, 2)
WHERE q.folio IN ('COT-R2046', 'COT-R2048', 'COT-R2050', 'COT-R2051', 'COT-R2039', 'COT-R2044');

-- Traslados: se limpian y se vuelven a insertar
DELETE FROM traslados;

SET @opt_luis   = (SELECT id FROM personal WHERE codigo = 'OPT-10');
SET @opt_ana    = (SELECT id FROM personal WHERE codigo = 'OPT-11');
SET @opt_ramon  = (SELECT id FROM personal WHERE codigo = 'OPT-12');
SET @opt_hector = (SELECT id FROM personal WHERE codigo = 'OPT-13');

SET @renta_2046 = (SELECT id FROM rentas WHERE folio = 'R-2046');
SET @renta_2048 = (SELECT id FROM rentas WHERE folio = 'R-2048');
SET @renta_2050 = (SELECT id FROM rentas WHERE folio = 'R-2050');
SET @renta_2051 = (SELECT id FROM rentas WHERE folio = 'R-2051');
SET @renta_2039 = (SELECT id FROM rentas WHERE folio = 'R-2039');
SET @renta_2044 = (SELECT id FROM rentas WHERE folio = 'R-2044');

INSERT INTO traslados
(folio, renta_id, tipo, fecha_hora_inicio, fecha_hora_fin, responsable_id, estado, fecha_asignacion, fecha_inicio_real)
VALUES
  -- TR-0086: Entrega hoy 08:00 - 09:30, Héctor Quintero (Ya inició)
  ('TR-0086', @renta_2046, 'ENTREGA',
   TIMESTAMP(CURDATE(), '08:00:00'), TIMESTAMP(CURDATE(), '09:30:00'),
   @opt_hector, 'EN_CURSO', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '17:00:00'), TIMESTAMP(CURDATE(), '08:05:00')),

  -- TR-0087: Entrega hoy 10:00 - 12:00, Luis Valenzuela (Cambiar)
  ('TR-0087', @renta_2048, 'ENTREGA',
   TIMESTAMP(CURDATE(), '10:00:00'), TIMESTAMP(CURDATE(), '12:00:00'),
   @opt_luis, 'PROGRAMADO', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '17:15:00'), NULL),

  -- TR-0092: Entrega hoy 13:00 - 14:30, Luis Valenzuela (Cambiar)
  ('TR-0092', @renta_2050, 'ENTREGA',
   TIMESTAMP(CURDATE(), '13:00:00'), TIMESTAMP(CURDATE(), '14:30:00'),
   @opt_luis, 'PROGRAMADO', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '17:30:00'), NULL),

  -- TR-0088: Entrega hoy 15:00 - 17:00, Sin asignar (empalma con TR-0093 si se intenta asignar a OPT-10 Luis)
  ('TR-0088', @renta_2051, 'ENTREGA',
   TIMESTAMP(CURDATE(), '15:00:00'), TIMESTAMP(CURDATE(), '17:00:00'),
   NULL, 'PROGRAMADO', NULL, NULL),

  -- TR-0093: Recolección hoy 16:00 - 17:30, Luis Valenzuela (Cambiar)
  ('TR-0093', @renta_2039, 'RECOLECCION',
   TIMESTAMP(CURDATE(), '16:00:00'), TIMESTAMP(CURDATE(), '17:30:00'),
   @opt_luis, 'PROGRAMADO', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '17:45:00'), NULL),

  -- TR-0089: Recolección mañana 09:00 - 11:00, Sin asignar (Asignar)
  ('TR-0089', @renta_2044, 'RECOLECCION',
   TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '09:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '11:00:00'),
   NULL, 'PROGRAMADO', NULL, NULL),

  -- TR-0094: Recolección en 12 días 16:00 - 18:00, Ramón Félix (Cambiar)
  ('TR-0094', @renta_2048, 'RECOLECCION',
   TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '16:00:00'), TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '18:00:00'),
   @opt_ramon, 'PROGRAMADO', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '18:00:00'), NULL);
