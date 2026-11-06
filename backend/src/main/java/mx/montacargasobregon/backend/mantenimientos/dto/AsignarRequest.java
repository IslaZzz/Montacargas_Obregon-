package mx.montacargasobregon.backend.mantenimientos.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Asignar uno o varios mantenimientos a un mecánico. */
public record AsignarRequest(
        @NotEmpty(message = "Debe indicarse al menos un mantenimiento") List<Long> mantenimientoIds,
        @NotNull(message = "El mecánico es obligatorio") Long personalId) {
}