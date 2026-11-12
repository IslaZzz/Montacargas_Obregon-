package mx.montacargasobregon.backend.revisiones.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.montacargasobregon.backend.revisiones.modelo.ResultadoRevision;

/**
 * Lo que reporta el mecánico al terminar una revisión.
 * Las notas son obligatorias cuando requiere mantenimiento, porque se vuelven la descripción de la falla.
 */
public record RegistrarResultadoRequest(
        @NotNull(message = "Elige el resultado de la revisión") ResultadoRevision resultado,
        @Size(max = 255, message = "Las notas no pueden pasar de 255 caracteres") String notas) {
}