package mx.montacargasobregon.backend.movil.mantenimientos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Por qué no se pudo completar una tarea, para el auxiliar. */
public record IncidenciaRequest(
        @NotBlank(message = "Escribe el mensaje para el auxiliar")
        @Size(max = 500, message = "El mensaje admite máximo 500 caracteres") String mensaje) {
}