package mx.montacargasobregon.backend.contratos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import mx.montacargasobregon.backend.contratos.modelo.Contrato;
import mx.montacargasobregon.backend.cotizaciones.dto.CotizacionResponse;

/**
 * Detalle del contrato (HU-23). Cliente, razón social, equipos, fechas, lugar y precio vienen en la cotización
 * aceptada de la que salió.
 */
public record ContratoResponse(
        Long id,
        String folio,
        String estado,
        LocalDateTime fechaGeneracion,
        LocalDate fechaLimiteAnticipo, // Si no se paga antes, ese día vence el apartado
        BigDecimal anticipo, // 30 % del total con IVA
        CotizacionResponse cotizacion) {

    public static ContratoResponse toContratoResponse(Contrato k) {
        return new ContratoResponse(
                k.getId(),
                k.getFolio(),
                k.getEstado().name(),
                k.getFechaHoraGeneracion(),
                k.getFechaLimiteAnticipo(),
                k.getAnticipo(),
                CotizacionResponse.toCotizacionResponse(k.getCotizacion()));
    }
}
