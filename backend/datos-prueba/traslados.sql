-- Datos de prueba para la sección de Traslados (HU: Asignar traslados y responsables).
-- Ejecutar después de arrancar el backend al menos una vez (crea la tabla traslados).
-- Se puede correr varias veces, no duplica clientes ni personal y reinicia los traslados.

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

-- Rentas correspondientes a los traslados
INSERT IGNORE INTO rentas
  (folio, cliente_id, fecha_inicio, fecha_fin, entrega_desde, entrega_hasta,
   recoleccion_desde, recoleccion_hasta, direccion_obra, contacto_obra, telefono_contacto, estado)
SELECT d.folio, c.id, d.inicio, d.fin, d.ent_desde, d.ent_hasta,
       d.rec_desde, d.rec_hasta, d.direccion, d.contacto, d.tel, d.estado
FROM (
         SELECT 'R-2046' AS folio, 'compras@industriascajeme.mx' AS correo,
                '2026-10-07' AS inicio, '2026-10-25' AS fin,
                '2026-10-07 08:00:00' AS ent_desde, '2026-10-07 09:30:00' AS ent_hasta,
                CAST(NULL AS DATETIME) AS rec_desde, CAST(NULL AS DATETIME) AS rec_hasta,
                'Blvd. Rodolfo Elías Calles 1520, Cd. Obregón' AS direccion,
                'Ramón Gutiérrez' AS contacto, '6441000001' AS tel, 'EN_CURSO' AS estado
         UNION ALL SELECT 'R-2048', 'obra@constructorayaqui.mx',
                '2026-10-07', '2026-10-19',
                '2026-10-07 10:00:00', '2026-10-07 12:00:00',
                '2026-10-19 16:00:00', '2026-10-19 18:00:00',
                'Calle 200 s/n, Valle del Yaqui, Cajeme',
                'Laura Méndez', '6441000002', 'PROGRAMADA'
         UNION ALL SELECT 'R-2050', 'almacen@bodegasnainari.mx',
                '2026-10-07', '2026-10-28',
                '2026-10-07 13:00:00', '2026-10-07 14:30:00',
                CAST(NULL AS DATETIME), CAST(NULL AS DATETIME),
                'Av. Náinari 845, Col. Centro, Cd. Obregón',
                'Jesús Rivera', '6441000003', 'PROGRAMADA'
         UNION ALL SELECT 'R-2051', 'contacto@empacadoranoroeste.mx',
                '2026-10-07', '2026-10-22',
                '2026-10-07 15:00:00', '2026-10-07 17:00:00',
                CAST(NULL AS DATETIME), CAST(NULL AS DATETIME),
                'Calle Norte 300, Parque Industrial, Cd. Obregón',
                'Carlos Vega', '6441000005', 'PROGRAMADA'
         UNION ALL SELECT 'R-2039', 'ana@agricolavalle.mx',
                '2026-09-20', '2026-10-07',
                '2026-09-20 09:00:00', '2026-09-20 11:00:00',
                '2026-10-07 16:00:00', '2026-10-07 17:30:00',
                'Carretera a Esperanza km 6, Cajeme',
                'Ana Castro', '6441000004', 'EN_CURSO'
         UNION ALL SELECT 'R-2044', 'reservas@hotelvallegrande.mx',
                '2026-09-25', '2026-10-08',
                '2026-09-25 10:00:00', '2026-09-25 12:00:00',
                '2026-10-08 09:00:00', '2026-10-08 11:00:00',
                'Calle Guerrero 402 Ote., Cd. Obregón',
                'Juan Pérez', '6441000006', 'EN_CURSO'
     ) d
         JOIN clientes c ON c.correo = d.correo;

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
  -- TR-0086: Entrega Mié 07 oct 08:00 - 09:30, Héctor Quintero (Ya inició)
  ('TR-0086', @renta_2046, 'ENTREGA', '2026-10-07 08:00:00', '2026-10-07 09:30:00', @opt_hector, 'EN_CURSO', '2026-10-06 17:00:00', '2026-10-07 08:05:00'),

  -- TR-0087: Entrega Mié 07 oct 10:00 - 12:00, Luis Valenzuela (Cambiar)
  ('TR-0087', @renta_2048, 'ENTREGA', '2026-10-07 10:00:00', '2026-10-07 12:00:00', @opt_luis, 'PROGRAMADO', '2026-10-06 17:15:00', NULL),

  -- TR-0092: Entrega Mié 07 oct 13:00 - 14:30, Luis Valenzuela (Cambiar)
  ('TR-0092', @renta_2050, 'ENTREGA', '2026-10-07 13:00:00', '2026-10-07 14:30:00', @opt_luis, 'PROGRAMADO', '2026-10-06 17:30:00', NULL),

  -- TR-0088: Entrega Mié 07 oct 15:00 - 17:00, Sin asignar (Asignar)
  ('TR-0088', @renta_2051, 'ENTREGA', '2026-10-07 15:00:00', '2026-10-07 17:00:00', NULL, 'PROGRAMADO', NULL, NULL),

  -- TR-0093: Recolección Mié 07 oct 16:00 - 17:30, Luis Valenzuela (Cambiar)
  ('TR-0093', @renta_2039, 'RECOLECCION', '2026-10-07 16:00:00', '2026-10-07 17:30:00', @opt_luis, 'PROGRAMADO', '2026-10-06 17:45:00', NULL),

  -- TR-0089: Recolección Jue 08 oct 09:00 - 11:00, Sin asignar (Asignar)
  ('TR-0089', @renta_2044, 'RECOLECCION', '2026-10-08 09:00:00', '2026-10-08 11:00:00', NULL, 'PROGRAMADO', NULL, NULL),

  -- TR-0094: Recolección Lun 19 oct 16:00 - 18:00, Ramón Félix (Cambiar)
  ('TR-0094', @renta_2048, 'RECOLECCION', '2026-10-19 16:00:00', '2026-10-19 18:00:00', @opt_ramon, 'PROGRAMADO', '2026-10-06 18:00:00', NULL);

