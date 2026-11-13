package mx.montacargasobregon.backend.movil.sesion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El código que se escribe en la pantalla de entrada de la app móvil. */
public record IngresarCodigoRequest(
        @NotBlank(message = "Escribe tu código")
        @Size(max = 20, message = "El código admite máximo 20 caracteres") String codigo) {
}