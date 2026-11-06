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

-- Rentas, con fechas relativas a hoy
INSERT IGNORE INTO rentas
  (folio, cliente_id, fecha_inicio, fecha_fin, entrega_desde, entrega_hasta,
   recoleccion_desde, recoleccion_hasta, direccion_obra, contacto_obra, telefono_contacto, estado)
SELECT d.folio, c.id, d.inicio, d.fin, d.ent_desde, d.ent_hasta,
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
         JOIN clientes c ON c.correo = d.correo;

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