-- Datos de prueba para sección de Revisiones.
-- Requiere: arrancar el backend una vez y correr antes mantenimientos.sql
-- (crea categorías, modelos, equipos y personal).
-- Si se corre mantenimientos.sql después de este, se pierde el enlace con las revisiones.

USE montacargas_obregon;

-- Equipos extra, para tener rentas con varios equipos
INSERT IGNORE INTO equipos (numero_unidad, numero_serie, anio_compra, estado, modelo_id)
SELECT d.unidad, d.serie, d.anio, 'OPERATIVO', m.id
FROM (
         SELECT 'MC-030' AS unidad, 'TY25-000030' AS serie, 2022 AS anio, '8FGU25' AS modelo
         UNION ALL SELECT 'MC-031', 'TY25-000031',  2022, '8FGU25'
         UNION ALL SELECT 'MC-032', 'HY50-000032',  2021, 'H50FT'
         UNION ALL SELECT 'MC-033', 'NS1F2-000033', 2020, '1F2'
         UNION ALL SELECT 'MC-034', 'TY30-000034',  2023, '8FGU30'
         UNION ALL SELECT 'MC-035', 'HY50-000035',  2019, 'H50FT'
         UNION ALL SELECT 'GE-005', 'CU100-000005', 2021, 'C100D5'
     ) d
         JOIN modelos m ON m.codigo = d.modelo;

-- Clientes: solo si no existe uno con ese correo
INSERT INTO clientes (nombre, apellido_paterno, apellido_materno, empresa, telefono, correo, fecha_registro)
SELECT d.nombre, d.paterno, NULL, d.empresa, d.telefono, d.correo, CURDATE()
FROM (
         SELECT 'Ramón' AS nombre, 'Gutiérrez' AS paterno, 'Industrias Cajeme' AS empresa,
                '6441000001' AS telefono, 'compras@industriascajeme.mx' AS correo
         UNION ALL SELECT 'Laura', 'Méndez', 'Constructora del Yaqui',   '6441000002', 'obra@constructorayaqui.mx'
         UNION ALL SELECT 'Jesús', 'Rivera', 'Bodegas Náinari',          '6441000003', 'almacen@bodegasnainari.mx'
         UNION ALL SELECT 'Ana',   'Castro', 'Agrícola Valle del Yaqui', '6441000004', 'ana@agricolavalle.mx'
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

SET @cliente = (SELECT id FROM clientes WHERE correo = 'ana@agricolavalle.mx' ORDER BY id LIMIT 1);
SET @falta = @cliente IS NOT NULL AND NOT EXISTS (SELECT 1 FROM razones_sociales WHERE cliente_id = @cliente);
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Carretera a Esperanza', 'km 6', 'Campo agrícola', 'Cajeme', '85210' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Agrícola Valle del Yaqui S.P.R. de R.L.', 'AVY180404GH4', LAST_INSERT_ID(), @cliente FROM DUAL WHERE @falta;

-- Toda renta sale de una cotización aceptada (HU-37): COT-R2061 es la de R-2061.
-- Se crean solo si no existen. Sus equipos y totales se llenan después, con los de la renta.
SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2061'),
    @entrega = TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '08:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Blvd. Rodolfo Elías Calles', '1520', 'Centro', 'Cd. Obregón', 'Sonora', '85000' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2061', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Ramón Gutiérrez', '6441000001', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'compras@industriascajeme.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2070'),
    @entrega = TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '10:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 15 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Calle 200', 's/n', 'Valle del Yaqui', 'Cajeme', 'Sonora', '85203' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2070', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Laura Méndez', '6441000002', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'obra@constructorayaqui.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2071'),
    @entrega = TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '13:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '18:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Av. Náinari', '845', 'Centro', 'Cd. Obregón', 'Sonora', '85000' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2071', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Jesús Rivera', '6441000003', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'almacen@bodegasnainari.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R2052'),
    @entrega = TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '08:00:00'), @recoleccion = TIMESTAMP(CURDATE(), '16:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Carretera a Esperanza', 'km 6', 'Campo agrícola', 'Cajeme', 'Sonora', '85210' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R2052', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Ana Castro', '6441000004', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'ana@agricolavalle.mx' AND @falta;

-- Rentas, con fechas relativas a hoy
INSERT IGNORE INTO rentas
  (folio, cliente_id, cotizacion_id, fecha_inicio, fecha_fin, entrega_desde, entrega_hasta,
   recoleccion_desde, recoleccion_hasta, direccion_obra, contacto_obra, telefono_contacto, estado)
SELECT d.folio, c.id, q.id, d.inicio, d.fin, d.ent_desde, d.ent_hasta,
       d.rec_desde, d.rec_hasta, d.direccion, d.contacto, d.tel, d.estado
FROM (
         SELECT 'R-2061' AS folio, 'compras@industriascajeme.mx' AS correo,
                CURDATE() + INTERVAL 2 DAY AS inicio, CURDATE() + INTERVAL 9 DAY AS fin,
             TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '08:00:00') AS ent_desde,
             TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '09:30:00') AS ent_hasta,
             CAST(NULL AS DATETIME) AS rec_desde, CAST(NULL AS DATETIME) AS rec_hasta,
             'Blvd. Rodolfo Elías Calles 1520, Cd. Obregón' AS direccion,
             'Ramón Gutiérrez' AS contacto, '6441000001' AS tel, 'PROGRAMADA' AS estado
         UNION ALL SELECT 'R-2070', 'obra@constructorayaqui.mx',
             CURDATE() + INTERVAL 1 DAY, CURDATE() + INTERVAL 15 DAY,
             TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '10:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '12:00:00'),
             NULL, NULL, 'Calle 200 s/n, Valle del Yaqui, Cajeme', 'Laura Méndez', '6441000002', 'PROGRAMADA'
         UNION ALL SELECT 'R-2071', 'almacen@bodegasnainari.mx',
             CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 10 DAY,
             TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '13:00:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '14:30:00'),
             NULL, NULL, 'Av. Náinari 845, Col. Centro, Cd. Obregón', 'Jesús Rivera', '6441000003', 'PROGRAMADA'
         UNION ALL SELECT 'R-2052', 'ana@agricolavalle.mx',
             CURDATE() - INTERVAL 10 DAY, CURDATE(),
             TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '09:30:00'),
             TIMESTAMP(CURDATE(), '16:00:00'), TIMESTAMP(CURDATE(), '17:30:00'),
             'Carretera a Esperanza km 6, Cajeme', 'Ana Castro', '6441000004', 'EN_CURSO'
     ) d
         JOIN clientes c ON c.correo = d.correo
         JOIN cotizaciones q ON q.folio = CONCAT('COT-', REPLACE(d.folio, '-', ''));

-- Equipos de cada renta
INSERT IGNORE INTO equipos_renta (renta_id, equipo_id, fase)
SELECT r.id, e.id, d.fase
FROM (
         SELECT 'R-2061' AS folio, 'MC-014' AS unidad, 'EN_REVISION_SALIDA' AS fase
         UNION ALL SELECT 'R-2061', 'MC-030', 'LISTO_ENTREGA'
         UNION ALL SELECT 'R-2070', 'MC-031', 'RESERVADO'
         UNION ALL SELECT 'R-2070', 'MC-032', 'RESERVADO'
         UNION ALL SELECT 'R-2070', 'GE-005', 'RESERVADO'
         UNION ALL SELECT 'R-2071', 'MC-033', 'EN_REVISION_SALIDA'
         UNION ALL SELECT 'R-2071', 'MC-034', 'RESERVADO'
         UNION ALL SELECT 'R-2052', 'MC-035', 'EN_OPERACION'
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
WHERE r.folio IN ('R-2061', 'R-2070', 'R-2071', 'R-2052')
  AND NOT EXISTS (SELECT 1 FROM equipos_cotizacion ec
                  WHERE ec.cotizacion_id = r.cotizacion_id AND ec.equipo_id = er.equipo_id);

UPDATE cotizaciones q
    JOIN (SELECT cotizacion_id, SUM(importe) AS subtotal FROM equipos_cotizacion GROUP BY cotizacion_id) t
    ON t.cotizacion_id = q.id
SET q.subtotal = t.subtotal, q.iva = ROUND(t.subtotal * 0.16, 2), q.total = t.subtotal + ROUND(t.subtotal * 0.16, 2)
WHERE q.folio IN ('COT-R2061', 'COT-R2070', 'COT-R2071', 'COT-R2052');

-- Revisiones: se quitan los enlaces, se borran y se vuelven a crear
UPDATE mantenimientos SET revision_id = NULL;
DELETE FROM revisiones;

SET @rafael  = (SELECT id FROM personal WHERE codigo = 'MEC-10');
SET @ernesto = (SELECT id FROM personal WHERE codigo = 'MEC-13');

INSERT INTO revisiones
(equipo_renta_id, tipo, fecha_programada, mecanico_id, fecha_asignacion,
 fecha_inicio, fecha_fin, estado, resultado, notas)
SELECT er.id, d.tipo, d.programada, d.mecanico, d.asignada, d.inicio, d.fin, d.estado, d.resultado, d.notas
FROM (
         -- R-2061: ya revisada; MC-014 tuvo falla y de ahí salió su mantenimiento
         SELECT 'R-2061' AS folio, 'MC-014' AS unidad, 'SALIDA' AS tipo,
             TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '08:00:00') AS programada,
     @ernesto AS mecanico, TIMESTAMP(CURDATE(), '08:00:00') AS asignada,
     TIMESTAMP(CURDATE(), '08:40:00') AS inicio, TIMESTAMP(CURDATE(), '09:12:00') AS fin,
    'COMPLETADA' AS estado, 'REQUIERE_MANTENIMIENTO' AS resultado,
    'Fuga de aceite hidráulico' AS notas
UNION ALL SELECT 'R-2061', 'MC-030', 'SALIDA', TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '08:00:00'),
              @ernesto, TIMESTAMP(CURDATE(), '08:00:00'), TIMESTAMP(CURDATE(), '09:15:00'),
              TIMESTAMP(CURDATE(), '09:40:00'), 'COMPLETADA', 'SIN_NOVEDAD', NULL

          -- R-2070: nadie asignado todavía. Es la que sirve para probar "asignar la renta completa"
          UNION ALL SELECT 'R-2070', 'MC-031', 'SALIDA', TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '10:00:00'),
                        NULL, NULL, NULL, NULL, 'PENDIENTE', NULL, NULL
                    UNION ALL SELECT 'R-2070', 'MC-032', 'SALIDA', TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '10:00:00'),
                                  NULL, NULL, NULL, NULL, 'PENDIENTE', NULL, NULL
                              UNION ALL SELECT 'R-2070', 'GE-005', 'SALIDA', TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '10:00:00'),
                                            NULL, NULL, NULL, NULL, 'PENDIENTE', NULL, NULL

                                        -- R-2071: Rafael ya empezó una de las dos
                                        UNION ALL SELECT 'R-2071', 'MC-033', 'SALIDA', TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '13:00:00'),
                                                      @rafael, TIMESTAMP(CURDATE(), '10:00:00'), TIMESTAMP(CURDATE(), '11:00:00'), NULL,
                                                      'EN_PROCESO', NULL, NULL
                                                  UNION ALL SELECT 'R-2071', 'MC-034', 'SALIDA', TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '13:00:00'),
                                                                @rafael, TIMESTAMP(CURDATE(), '10:00:00'), NULL, NULL, 'ASIGNADO', NULL, NULL

                                                            -- R-2052: el equipo regresa hoy, revisión de entrada sin asignar
                                                            UNION ALL SELECT 'R-2052', 'MC-035', 'ENTRADA', TIMESTAMP(CURDATE(), '17:30:00'),
                                                                          NULL, NULL, NULL, NULL, 'PENDIENTE', NULL, NULL
                                                                          ) d
                                                                          JOIN rentas r ON r.folio = d.folio
                                                                          JOIN equipos e ON e.numero_unidad = d.unidad
                                                                          JOIN equipos_renta er ON er.renta_id = r.id AND er.equipo_id = e.id;

-- El mantenimiento de MC-014 nació de su revisión de salida
UPDATE mantenimientos m
    JOIN revisiones rv ON rv.resultado = 'REQUIERE_MANTENIMIENTO'
    JOIN equipos_renta er ON er.id = rv.equipo_renta_id AND er.equipo_id = m.equipo_id
    SET m.revision_id = rv.id;