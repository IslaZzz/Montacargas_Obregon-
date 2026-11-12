-- Requiere correr antes cotizaciones.sql: crea los clientes, sus razones sociales y los equipos.

USE montacargas_obregon;

-- Toda renta sale de una cotización aceptada (HU-37): COT-R2061 es la de R-2061.
-- Se crean solo si no existen. Sus equipos y totales se llenan después, con los de la renta.
SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R3001'),
    @entrega = TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '08:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '16:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Calle 200', '1500', 'Centro', 'Cd. Obregón', 'Sonora', '85000' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R3001', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Juan Pérez', '6441234567', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'juan.perez@constructorayaqui.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R3002'),
    @entrega = TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '08:00:00'), @recoleccion = TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '16:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Blvd. Rodolfo Elías Calles', '845', 'Centro', 'Cd. Obregón', 'Sonora', '85000' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R3002', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'María López', '6449876543', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'compras@obrasdelmayo.mx' AND @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM cotizaciones WHERE folio = 'COT-R3003'),
    @entrega = TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '08:00:00'), @recoleccion = TIMESTAMP(CURDATE(), '16:00:00');
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
SELECT 'Calle Jalisco', '312', 'Centro', 'Navojoa', 'Sonora', '85800' FROM DUAL WHERE @falta;
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id,
                          contacto_obra, telefono_contacto, traslado_entrega, traslado_recoleccion,
                          subtotal, iva, total, estado, fecha_creacion, fecha_envio, fecha_respuesta)
SELECT 'COT-R3003', c.id, (SELECT MIN(rs.id) FROM razones_sociales rs WHERE rs.cliente_id = c.id),
       @entrega, @recoleccion, LAST_INSERT_ID(), 'Carlos Ramírez', '6445551234', 0.00, 0.00,
       0.00, 0.00, 0.00, 'ACEPTADA',
       @entrega - INTERVAL 7 DAY, @entrega - INTERVAL 6 DAY, DATE(@entrega - INTERVAL 5 DAY)
FROM clientes c WHERE c.correo = 'carlos.ramirez@correo.com' AND @falta;

-- R-3001: Apartado próximo
INSERT INTO rentas (
    folio, cliente_id, cotizacion_id, fecha_inicio, fecha_fin,
    entrega_desde, entrega_hasta,
    recoleccion_desde, recoleccion_hasta,
    direccion_obra, contacto_obra, telefono_contacto, estado
)
SELECT
    'R-3001', c.id, (SELECT id FROM cotizaciones WHERE folio = 'COT-R3001'),
    CURDATE() + INTERVAL 2 DAY,
    CURDATE() + INTERVAL 9 DAY,
    TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '08:00:00'),
    TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '09:30:00'),
    TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '16:00:00'),
    TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '17:00:00'),
    'Calle 200 No. 1500, Cd. Obregón',
    'Juan Pérez', '6441234567', 'PROGRAMADA'
FROM clientes c
WHERE c.correo = 'juan.perez@constructorayaqui.mx'
  AND NOT EXISTS (
      SELECT 1 FROM rentas WHERE folio = 'R-3001'
  );

-- R-3002: Renta en curso, próxima a vencer
INSERT INTO rentas (
    folio, cliente_id, cotizacion_id, fecha_inicio, fecha_fin,
    entrega_desde, entrega_hasta,
    recoleccion_desde, recoleccion_hasta,
    direccion_obra, contacto_obra, telefono_contacto, estado
)
SELECT
    'R-3002', c.id, (SELECT id FROM cotizaciones WHERE folio = 'COT-R3002'),
    CURDATE() - INTERVAL 5 DAY,
    CURDATE() + INTERVAL 2 DAY,
    TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '08:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '09:00:00'),
    TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '16:00:00'),
    TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '17:00:00'),
    'Blvd. Rodolfo Elías Calles 845, Cd. Obregón',
    'María López', '6449876543', 'EN_CURSO'
FROM clientes c
WHERE c.correo = 'compras@obrasdelmayo.mx'
  AND NOT EXISTS (
      SELECT 1 FROM rentas WHERE folio = 'R-3002'
  );

-- R-3003: Renta con recolección pendiente
INSERT INTO rentas (
    folio, cliente_id, cotizacion_id, fecha_inicio, fecha_fin,
    entrega_desde, entrega_hasta,
    recoleccion_desde, recoleccion_hasta,
    direccion_obra, contacto_obra, telefono_contacto, estado
)
SELECT
    'R-3003', c.id, (SELECT id FROM cotizaciones WHERE folio = 'COT-R3003'),
    CURDATE() - INTERVAL 7 DAY,
    CURDATE(),
    TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '08:00:00'),
    TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '09:00:00'),
    TIMESTAMP(CURDATE(), '16:00:00'),
    TIMESTAMP(CURDATE(), '17:00:00'),
    'Calle Jalisco 312, Navojoa',
    'Carlos Ramírez', '6445551234', 'EN_CURSO'
FROM clientes c
WHERE c.correo = 'carlos.ramirez@correo.com'
  AND NOT EXISTS (
      SELECT 1 FROM rentas WHERE folio = 'R-3003'
  );

-- Relacionar equipos con sus rentas
INSERT IGNORE INTO equipos_renta (renta_id, equipo_id, fase)
SELECT r.id, e.id, datos.fase
FROM (
    SELECT 'R-3001' AS folio, 'MC-07' AS unidad, 'RESERVADO' AS fase
    UNION ALL
    SELECT 'R-3002', 'GR-02', 'EN_OPERACION'
    UNION ALL
    SELECT 'R-3003', 'PT-03', 'EN_OPERACION'
) datos
JOIN rentas r ON r.folio = datos.folio
JOIN equipos e ON e.numero_unidad = datos.unidad;

-- Cada cotización lleva los equipos de su renta, con la tarifa del modelo; de ahí salen sus totales (IVA 16%)
INSERT INTO equipos_cotizacion (equipo_id, tarifa_dia, importe, cotizacion_id)
SELECT er.equipo_id, m.tarifa_dia, m.tarifa_dia * (DATEDIFF(r.fecha_fin, r.fecha_inicio) + 1), r.cotizacion_id
FROM rentas r
         JOIN equipos_renta er ON er.renta_id = r.id
         JOIN equipos e ON e.id = er.equipo_id
         JOIN modelos m ON m.id = e.modelo_id
WHERE r.folio IN ('R-3001', 'R-3002', 'R-3003')
  AND NOT EXISTS (SELECT 1 FROM equipos_cotizacion ec
                  WHERE ec.cotizacion_id = r.cotizacion_id AND ec.equipo_id = er.equipo_id);

UPDATE cotizaciones q
    JOIN (SELECT cotizacion_id, SUM(importe) AS subtotal FROM equipos_cotizacion GROUP BY cotizacion_id) t
    ON t.cotizacion_id = q.id
SET q.subtotal = t.subtotal, q.iva = ROUND(t.subtotal * 0.16, 2), q.total = t.subtotal + ROUND(t.subtotal * 0.16, 2)
WHERE q.folio IN ('COT-R3001', 'COT-R3002', 'COT-R3003');
