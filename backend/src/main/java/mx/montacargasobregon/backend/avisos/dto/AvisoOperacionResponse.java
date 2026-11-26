package mx.montacargasobregon.backend.avisos.dto;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.avisos.modelo.AvisoOperacion;
import mx.montacargasobregon.backend.avisos.modelo.TipoAvisoOperacion;

/** Un aviso de la operación, como se muestra en la campana o en la app. rentaId es para ir a la renta. */
public record AvisoOperacionResponse(Long id, TipoAvisoOperacion tipo, String titulo, String detalle,
                                     Long rentaId, LocalDateTime fecha, boolean leido) {

    public static AvisoOperacionResponse de(AvisoOperacion aviso) {
        return new AvisoOperacionResponse(
                aviso.getId(),
                aviso.getTipo(),
                aviso.getTitulo(),
                aviso.getDetalle(),
                aviso.getRenta() == null ? null : aviso.getRenta().getId(),
                aviso.getFecha(),
                aviso.isLeido());
    }
}