package mx.montacargasobregon.backend.traslados.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarTrasladoRequest(
        @NotNull(message = "Debes seleccionar un operador responsable")
        Long personalId
) {
}

