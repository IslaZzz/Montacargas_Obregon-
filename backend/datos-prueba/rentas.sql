USE montacargas_obregon;

-- R-3001: Apartado próximo
INSERT INTO rentas (
    folio, cliente_id, fecha_inicio, fecha_fin,
    entrega_desde, entrega_hasta,
    recoleccion_desde, recoleccion_hasta,
    direccion_obra, contacto_obra, telefono_contacto, estado
)
SELECT
    'R-3001', c.id,
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
    folio, cliente_id, fecha_inicio, fecha_fin,
    entrega_desde, entrega_hasta,
    recoleccion_desde, recoleccion_hasta,
    direccion_obra, contacto_obra, telefono_contacto, estado
)
SELECT
    'R-3002', c.id,
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
    folio, cliente_id, fecha_inicio, fecha_fin,
    entrega_desde, entrega_hasta,
    recoleccion_desde, recoleccion_hasta,
    direccion_obra, contacto_obra, telefono_contacto, estado
)
SELECT
    'R-3003', c.id,
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
