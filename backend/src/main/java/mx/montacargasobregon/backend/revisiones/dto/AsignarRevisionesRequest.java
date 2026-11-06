package mx.montacargasobregon.backend.revisiones.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Asignar una o varias revisiones a un mecánico; para una renta completa, todas las suyas. */
public record AsignarRevisionesRequest(
        @NotEmpty(message = "Debe indicarse al menos una revisión") List<Long> revisionIds,
        @NotNull(message = "El mecánico es obligatorio") Long personalId) {
}