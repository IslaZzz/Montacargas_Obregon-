package mx.montacargasobregon.backend.cartas.dto;

import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;

/**
 * La carta responsiva de una renta. Si no se ha generado: estado SIN_GENERAR y sin versión ni archivo.
 *
 * @param correoCliente   a dónde se envía (el correo registrado del cliente)
 * @param puedeGenerar    si se puede generar o regenerar: la renta está Programada y la carta no está firmada
 * @param puedeEnviar     si se puede enviar o reenviar: además de lo anterior, ya tiene archivo
 * @param fechaEnvio      cuándo se envió la versión vigente; vacía si no se ha enviado
 * @param correoEnvio     a qué correo se envió la versión vigente
 * @param mensajeSugerido el texto con que se llena el mensaje al enviarla
 * @param puedeFirmar     si se puede registrar la firma: ya tiene archivo, no está firmada
 *                        y la renta está Programada o En curso
 * @param firmanteSugerido con quién se llena "Nombre de quien firma" (el contacto en la obra)
 * @param firma           la firma registrada; vacía si no está firmada
 * @param cambios         los datos de la renta que cambiaron después de generar la carta
 */
public record CartaResponse(
        Long rentaId,
        String folio,
        EstadoRenta estadoRenta,
        EstadoCarta estado,
        Integer version,
        String nombreArchivo,
        LocalDateTime fechaGeneracion,
        String correoCliente,
        boolean tieneArchivo,
        boolean puedeGenerar,
        boolean puedeEnviar,
        LocalDateTime fechaEnvio,
        String correoEnvio,
        String mensajeSugerido,
        boolean puedeFirmar,
        String firmanteSugerido,
        FirmaCarta firma,
        List<CambioCarta> cambios) {
}
