-- Datos de prueba para Cotizaciones (HU-12, HU-18, HU-20 y HU-37).
-- Clientes y equipos de la hoja «Datos de prueba» de los casos de Delivery S1.
-- Ejecutar después de iniciar el backend una vez, para que existan las tablas.
-- Se puede correr varias veces: no duplica catálogos ni clientes y reinicia las cotizaciones de Comercial,
-- junto con las rentas que nacieron al aceptarlas.

USE montacargas_obregon;

-- Categorías
INSERT IGNORE INTO categorias (nombre) VALUES
  ('Montacargas'), ('Grúa'), ('Patín');

-- Modelos con su tarifa por día (sin IVA)
INSERT IGNORE INTO modelos (codigo, nombre, marca, descripcion, tarifa_dia, categoria_id)
SELECT d.codigo, d.nombre, d.marca, NULL, d.tarifa, c.id
FROM (
         SELECT '8FBE30' AS codigo, 'Montacargas eléctrico 3 t' AS nombre, 'Toyota' AS marca, 500.00 AS tarifa, 'Montacargas' AS categoria
         UNION ALL SELECT 'X-HIDUO 128', 'Grúa articulada 12 t',   'Hiab',  1200.00, 'Grúa'
         UNION ALL SELECT 'PTH50',       'Patín hidráulico 2.5 t', 'Crown',  150.00, 'Patín'
     ) d
         JOIN categorias c ON c.nombre = d.categoria;

-- Equipos
INSERT IGNORE INTO equipos (numero_unidad, numero_serie, anio_compra, estado, modelo_id)
SELECT d.unidad, d.serie, d.anio, 'OPERATIVO', m.id
FROM (
         SELECT 'MC-07' AS unidad, 'TY8FBE-000007' AS serie, 2022 AS anio, '8FBE30' AS modelo
         UNION ALL SELECT 'GR-02', 'HIAB128-000002', 2021, 'X-HIDUO 128'
         UNION ALL SELECT 'PT-03', 'CRPTH50-000003', 2023, 'PTH50'
     ) d
         JOIN modelos m ON m.codigo = d.modelo;

-- Clientes: se agregan solo si no existe uno con ese correo
INSERT INTO clientes (nombre, apellido_paterno, apellido_materno, empresa, telefono, correo, fecha_registro)
SELECT d.nombre, d.paterno, d.materno, d.empresa, d.telefono, d.correo, d.registro
FROM (
         SELECT 'Juan' AS nombre, 'Pérez' AS paterno, 'Valenzuela' AS materno, 'Constructora Yaqui' AS empresa,
                '6441234567' AS telefono, 'juan.perez@constructorayaqui.mx' AS correo, CURDATE() - INTERVAL 20 DAY AS registro
         UNION ALL SELECT 'María', 'López', 'Ruiz', 'Obras del Mayo',
                '6449876543', 'compras@obrasdelmayo.mx', CURDATE() - INTERVAL 15 DAY
         UNION ALL SELECT 'Carlos', 'Ramírez', 'Soto', 'Persona física',
                '6445551234', 'carlos.ramirez@correo.com', CURDATE() - INTERVAL 10 DAY
     ) d
WHERE NOT EXISTS (SELECT 1 FROM clientes c WHERE c.correo = d.correo);

SET @juan   = (SELECT id FROM clientes WHERE correo = 'juan.perez@constructorayaqui.mx' ORDER BY id LIMIT 1);
SET @maria  = (SELECT id FROM clientes WHERE correo = 'compras@obrasdelmayo.mx' ORDER BY id LIMIT 1);
SET @carlos = (SELECT id FROM clientes WHERE correo = 'carlos.ramirez@correo.com' ORDER BY id LIMIT 1);

-- Razones sociales con su dirección fiscal: se agregan solo si no existe ese RFC
SET @falta = NOT EXISTS (SELECT 1 FROM razones_sociales WHERE rfc = 'CYA010203AB1');
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Calle 200', '1500', 'Centro', 'Cd. Obregón', '85000' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Constructora Yaqui S.A. de C.V.', 'CYA010203AB1', LAST_INSERT_ID(), @juan FROM DUAL WHERE @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM razones_sociales WHERE rfc = 'OMA150610KX3');
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Blvd. Rodolfo Elías Calles', '845', 'Centro', 'Cd. Obregón', '85000' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Obras del Mayo S.A. de C.V.', 'OMA150610KX3', LAST_INSERT_ID(), @maria FROM DUAL WHERE @falta;

-- CL-02 tiene una segunda razón social
SET @falta = NOT EXISTS (SELECT 1 FROM razones_sociales WHERE rfc = 'OMM180920QW5');
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Blvd. Rodolfo Elías Calles', '845', 'Centro', 'Cd. Obregón', '85000' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Obras del Mayo Maquinaria S.A. de C.V.', 'OMM180920QW5', LAST_INSERT_ID(), @maria FROM DUAL WHERE @falta;

SET @falta = NOT EXISTS (SELECT 1 FROM razones_sociales WHERE rfc = 'RASC800101AB2');
INSERT INTO direcciones_fiscales (calle, numero, colonia, ciudad, codigo_postal)
SELECT 'Calle Jalisco', '312', 'Centro', 'Navojoa', '85800' FROM DUAL WHERE @falta;
INSERT INTO razones_sociales (nombre, rfc, direccion_fiscal_id, cliente_id)
SELECT 'Carlos Ramírez Soto', 'RASC800101AB2', LAST_INSERT_ID(), @carlos FROM DUAL WHERE @falta;

SET @rs_yaqui      = (SELECT id FROM razones_sociales WHERE rfc = 'CYA010203AB1' ORDER BY id LIMIT 1);
SET @rs_mayo       = (SELECT id FROM razones_sociales WHERE rfc = 'OMA150610KX3' ORDER BY id LIMIT 1);
SET @rs_maquinaria = (SELECT id FROM razones_sociales WHERE rfc = 'OMM180920QW5' ORDER BY id LIMIT 1);
SET @rs_carlos     = (SELECT id FROM razones_sociales WHERE rfc = 'RASC800101AB2' ORDER BY id LIMIT 1);

SET @mc07 = (SELECT id FROM equipos WHERE numero_unidad = 'MC-07');
SET @gr02 = (SELECT id FROM equipos WHERE numero_unidad = 'GR-02');
SET @pt03 = (SELECT id FROM equipos WHERE numero_unidad = 'PT-03');

-- Bases creadas antes de ajustar Cotizacion al diagrama de dominio: la dirección de la obra era un texto
-- y los equipos iban en partidas_cotizacion. Se quitan para guardar con la estructura nueva.
DROP TABLE IF EXISTS partidas_cotizacion;
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns
                      WHERE table_schema = DATABASE() AND table_name = 'cotizaciones' AND column_name = 'direccion_obra'),
              'ALTER TABLE cotizaciones DROP COLUMN direccion_obra', 'DO 0');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;

-- Hibernate no cambia columnas que ya existen: el estado admite los valores nuevos del diagrama
-- (En autorización, Rechazada y Vencida) y la fecha de envío guarda también la hora.
ALTER TABLE cotizaciones
  MODIFY estado ENUM('ACEPTADA', 'BORRADOR', 'EN_AUTORIZACION', 'ENVIADA', 'NO_ACEPTADA', 'RECHAZADA', 'VENCIDA') NOT NULL,
  MODIFY fecha_envio DATETIME(6) NULL;

-- Cotizaciones de Comercial (COT-0001…): se borran y se vuelven a crear, cada una con su dirección de obra.
-- Las COT-R… son las de las rentas de prueba de Operativo y se quedan.
-- Primero se borran las rentas que nacieron al aceptar alguna (HU-37), con lo que cuelga de cada una.
CREATE TEMPORARY TABLE rentas_a_borrar AS
SELECT r.id FROM rentas r JOIN cotizaciones q ON q.id = r.cotizacion_id WHERE q.folio NOT LIKE 'COT-R%';
UPDATE mantenimientos SET revision_id = NULL
WHERE revision_id IN (SELECT rv.id FROM revisiones rv
                          JOIN equipos_renta er ON er.id = rv.equipo_renta_id
                          JOIN rentas_a_borrar b ON b.id = er.renta_id);
DELETE rv FROM revisiones rv JOIN equipos_renta er ON er.id = rv.equipo_renta_id
    JOIN rentas_a_borrar b ON b.id = er.renta_id;
DELETE t FROM traslados t JOIN rentas_a_borrar b ON b.id = t.renta_id;
DELETE c FROM cartas_responsivas c JOIN rentas_a_borrar b ON b.id = c.renta_id;
DELETE er FROM equipos_renta er JOIN rentas_a_borrar b ON b.id = er.renta_id;
DELETE r FROM rentas r JOIN rentas_a_borrar b ON b.id = r.id;
DROP TEMPORARY TABLE rentas_a_borrar;
DELETE ec FROM equipos_cotizacion ec JOIN cotizaciones q ON q.id = ec.cotizacion_id WHERE q.folio NOT LIKE 'COT-R%';
DELETE FROM cotizaciones WHERE folio NOT LIKE 'COT-R%';
DELETE d FROM direcciones_obra d LEFT JOIN cotizaciones q ON q.direccion_obra_id = d.id WHERE q.id IS NULL;

-- Las fechas son relativas a hoy. Importe = días (contando entrega y recolección) × tarifa; IVA 16%.
-- Traslados en 0 y sin descuento: llegan con HU-14 y HU-15.

-- No aceptada (TC-20-09): GR-02 5 días
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
VALUES ('Calle 200', '1500', 'Centro', 'Cd. Obregón', 'Sonora', '85000');
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id, contacto_obra,
                          telefono_contacto, traslado_entrega, traslado_recoleccion, subtotal, iva, total, estado,
                          fecha_creacion, fecha_envio, fecha_respuesta)
VALUES ('COT-0001', @juan, @rs_yaqui,
        TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 16 DAY, '18:00:00'),
        LAST_INSERT_ID(), 'Ing. Raúl Soto', '6442104455', 0.00, 0.00,
        6000.00, 960.00, 6960.00, 'NO_ACEPTADA',
        TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '10:15:00'), TIMESTAMP(CURDATE() - INTERVAL 7 DAY, '10:30:00'),
        CURDATE() - INTERVAL 4 DAY);
INSERT INTO equipos_cotizacion (equipo_id, tarifa_dia, importe, cotizacion_id) VALUES
  (@gr02, 1200.00, 6000.00, LAST_INSERT_ID());

-- Aceptada (TC-20-09): PT-03 1 día, entrega y recolección el mismo día
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
VALUES ('Calle Jalisco', '312', 'Centro', 'Navojoa', 'Sonora', '85800');
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id, contacto_obra,
                          telefono_contacto, traslado_entrega, traslado_recoleccion, subtotal, iva, total, estado,
                          fecha_creacion, fecha_envio, fecha_respuesta)
VALUES ('COT-0002', @carlos, @rs_carlos,
        TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '07:30:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '19:00:00'),
        LAST_INSERT_ID(), 'Carlos Ramírez', '6445551234', 0.00, 0.00,
        150.00, 24.00, 174.00, 'ACEPTADA',
        TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '12:00:00'), TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '12:30:00'),
        CURDATE() - INTERVAL 3 DAY);
INSERT INTO equipos_cotizacion (equipo_id, tarifa_dia, importe, cotizacion_id) VALUES
  (@pt03, 150.00, 150.00, LAST_INSERT_ID());

-- Enviada, para marcar como aceptada (TC-20-04, 06 y 07): MC-07 3 días
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
VALUES ('Calle Jalisco', '312', 'Centro', 'Navojoa', 'Sonora', '85800');
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id, contacto_obra,
                          telefono_contacto, traslado_entrega, traslado_recoleccion, subtotal, iva, total, estado,
                          fecha_creacion, fecha_envio, fecha_respuesta)
VALUES ('COT-0003', @carlos, @rs_carlos,
        TIMESTAMP(CURDATE() + INTERVAL 6 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 8 DAY, '18:00:00'),
        LAST_INSERT_ID(), 'Carlos Ramírez', '6445551234', 0.00, 0.00,
        1500.00, 240.00, 1740.00, 'ENVIADA',
        TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '09:40:00'), TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '11:00:00'), NULL);
INSERT INTO equipos_cotizacion (equipo_id, tarifa_dia, importe, cotizacion_id) VALUES
  (@mc07, 500.00, 1500.00, LAST_INSERT_ID());

-- Enviada, para marcar como no aceptada (TC-20-08): GR-02 2 días
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
VALUES ('Calle 200', '1500', 'Centro', 'Cd. Obregón', 'Sonora', '85000');
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id, contacto_obra,
                          telefono_contacto, traslado_entrega, traslado_recoleccion, subtotal, iva, total, estado,
                          fecha_creacion, fecha_envio, fecha_respuesta)
VALUES ('COT-0004', @juan, @rs_yaqui,
        TIMESTAMP(CURDATE() + INTERVAL 7 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 8 DAY, '18:00:00'),
        LAST_INSERT_ID(), 'Ing. Raúl Soto', '6442104455', 0.00, 0.00,
        2400.00, 384.00, 2784.00, 'ENVIADA',
        TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '16:40:00'), TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '09:15:00'), NULL);
INSERT INTO equipos_cotizacion (equipo_id, tarifa_dia, importe, cotizacion_id) VALUES
  (@gr02, 1200.00, 2400.00, LAST_INSERT_ID());

-- Borrador, para el PDF y descargarlo (TC-18-02 a 06 y TC-20-03): MC-07 y GR-02 5 días, total $9,860.00
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
VALUES ('Carretera Internacional', 'km 4.5', 'Parque Industrial', 'Cd. Obregón', 'Sonora', '85065');
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id, contacto_obra,
                          telefono_contacto, traslado_entrega, traslado_recoleccion, subtotal, iva, total, estado,
                          fecha_creacion, fecha_envio, fecha_respuesta)
VALUES ('COT-0005', @maria, @rs_maquinaria,
        TIMESTAMP(CURDATE() + INTERVAL 5 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '18:00:00'),
        LAST_INSERT_ID(), 'Arq. Laura Medina', '6441112233', 0.00, 0.00,
        8500.00, 1360.00, 9860.00, 'BORRADOR',
        TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '11:20:00'), NULL, NULL);
SET @c = LAST_INSERT_ID();
INSERT INTO equipos_cotizacion (equipo_id, tarifa_dia, importe, cotizacion_id) VALUES
  (@mc07, 500.00, 2500.00, @c),
  (@gr02, 1200.00, 6000.00, @c);

-- Borrador, para compartir el PDF (TC-18-07): MC-07 5 días, total $2,900.00
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
VALUES ('Calle 200', '1500', 'Centro', 'Cd. Obregón', 'Sonora', '85000');
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id, contacto_obra,
                          telefono_contacto, traslado_entrega, traslado_recoleccion, subtotal, iva, total, estado,
                          fecha_creacion, fecha_envio, fecha_respuesta)
VALUES ('COT-0006', @juan, @rs_yaqui,
        TIMESTAMP(CURDATE() + INTERVAL 5 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 9 DAY, '18:00:00'),
        LAST_INSERT_ID(), 'Ing. Raúl Soto', '6442104455', 0.00, 0.00,
        2500.00, 400.00, 2900.00, 'BORRADOR',
        TIMESTAMP(CURDATE(), '09:30:00'), NULL, NULL);
INSERT INTO equipos_cotizacion (equipo_id, tarifa_dia, importe, cotizacion_id) VALUES
  (@mc07, 500.00, 2500.00, LAST_INSERT_ID());

-- Borrador sin equipos: «Ver PDF» deshabilitado (TC-18-08)
INSERT INTO direcciones_obra (calle, numero, colonia, ciudad, estado, codigo_postal)
VALUES ('Carretera Internacional', 'km 4.5', 'Parque Industrial', 'Cd. Obregón', 'Sonora', '85065');
INSERT INTO cotizaciones (folio, cliente_id, razon_social_id, fecha_entrega, fecha_recoleccion, direccion_obra_id, contacto_obra,
                          telefono_contacto, traslado_entrega, traslado_recoleccion, subtotal, iva, total, estado,
                          fecha_creacion, fecha_envio, fecha_respuesta)
VALUES ('COT-0007', @maria, @rs_mayo,
        TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '18:00:00'),
        LAST_INSERT_ID(), 'Arq. Laura Medina', '6441112233', 0.00, 0.00,
        0.00, 0.00, 0.00, 'BORRADOR',
        TIMESTAMP(CURDATE(), '10:10:00'), NULL, NULL);
