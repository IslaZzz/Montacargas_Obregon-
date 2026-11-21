package mx.montacargasobregon.backend.movil.mantenimientos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo que hizo el mecánico en una tarea. Solo el trabajo realizado es obligatorio. */
public record AtenderTareaRequest(
        @NotBlank(message = "Describe el trabajo realizado")
        @Size(max = 500, message = "El trabajo realizado admite máximo 500 caracteres") String trabajoRealizado,
        @Size(max = 255, message = "Las refacciones admiten máximo 255 caracteres") String refacciones,
        @Size(max = 50, message = "El tiempo invertido admite máximo 50 caracteres") String tiempoInvertido,
        @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres") String observaciones) {
}