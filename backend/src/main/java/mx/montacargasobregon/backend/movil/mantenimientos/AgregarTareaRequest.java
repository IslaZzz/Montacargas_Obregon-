package mx.montacargasobregon.backend.movil.mantenimientos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Algo que el mecánico encontró y no venía en el mantenimiento: se agrega como una tarea más. */
public record AgregarTareaRequest(
        @NotBlank(message = "Escribe qué encontraste")
        @Size(max = 100, message = "Lo que encontraste admite máximo 100 caracteres") String titulo,
        @Size(max = 100, message = "El componente admite máximo 100 caracteres") String componente,
        @Size(max = 255, message = "La acción sugerida admite máximo 255 caracteres") String accionSugerida) {
}