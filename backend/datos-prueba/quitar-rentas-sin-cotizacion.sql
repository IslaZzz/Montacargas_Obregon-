-- Una sola vez, al pasar a HU-37: toda renta sale de una cotización aceptada.
-- Borra las rentas que no tienen cotización (las de la captura manual y las de los scripts anteriores),
-- con lo que cuelga de cada una: equipos, revisiones (y su liga con mantenimientos), traslados y cartas.
--
-- Pasos:
--   1. Arrancar el backend una vez: agrega rentas.cotizacion_id (avisa que no pudo crear su llave; es normal).
--   2. Correr este script.
--   3. Volver a correr los scripts que crean rentas, en este orden: revisiones.sql, traslados.sql,
--      apartados-proximos.sql y rentas.sql (este último después de cotizaciones.sql).
--   4. Reiniciar el backend: ya crea la llave de rentas.cotizacion_id.

USE montacargas_obregon;

CREATE TEMPORARY TABLE rentas_a_borrar AS
SELECT r.id FROM rentas r LEFT JOIN cotizaciones q ON q.id = r.cotizacion_id WHERE q.id IS NULL;
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

-- Una cotización genera una sola renta. En una tabla que ya existía Hibernate no agrega esa regla: se agrega aquí
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.statistics
                      WHERE table_schema = DATABASE() AND table_name = 'rentas'
                        AND column_name = 'cotizacion_id' AND non_unique = 0),
              'DO 0', 'ALTER TABLE rentas ADD CONSTRAINT uk_rentas_cotizacion UNIQUE (cotizacion_id)');
PREPARE paso FROM @sql;
EXECUTE paso;
DEALLOCATE PREPARE paso;
