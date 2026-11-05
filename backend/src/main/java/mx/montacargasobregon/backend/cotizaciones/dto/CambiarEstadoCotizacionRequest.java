package mx.montacargasobregon.backend.cotizaciones.dto;

import jakarta.validation.constraints.NotNull;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;

/** Nuevo estado que se le asigna a una cotización. */
public record CambiarEstadoCotizacionRequest(
        @NotNull(message = "El estado es obligatorio") EstadoCotizacion estado) {
}
