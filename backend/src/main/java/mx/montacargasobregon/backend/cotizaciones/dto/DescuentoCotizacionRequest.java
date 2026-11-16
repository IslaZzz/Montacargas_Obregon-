package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.montacargasobregon.backend.cotizaciones.modelo.TipoDescuento;

/**
 * Cuerpo de PATCH /api/cotizaciones/5/descuento (HU-15).
 * Con NINGUNO, valor y motivo llegan en null.
 * Los límites que dependen del tipo (porcentaje menor a 100, monto menor al subtotal)
 * los revisa el servicio, porque aquí no se conoce el subtotal.
 */
public record DescuentoCotizacionRequest(
        @NotNull(message = "Elige si lleva descuento")
        TipoDescuento tipo,
        @DecimalMin(value = "0.00", message = "Descuento inválido")
        @Digits(integer = 10, fraction = 2, message = "Descuento inválido")
        BigDecimal valor,
        @Size(max = 200, message = "El motivo admite máximo 200 caracteres")
        String motivo) {
}
