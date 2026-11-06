package mx.montacargasobregon.backend.cartas.dto;

import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;

/**
 * La carta responsiva de una renta. Si no se ha generado: estado SIN_GENERAR y sin versión ni archivo.
 *
 * @param correoCliente a dónde se enviaría (el correo registrado del cliente)
 * @param puedeGenerar  si se puede generar o regenerar: la renta está Programada y la carta no está firmada
 * @param cambios       los datos de la renta que cambiaron después de generar la carta
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
        List<CambioCarta> cambios) {
}
