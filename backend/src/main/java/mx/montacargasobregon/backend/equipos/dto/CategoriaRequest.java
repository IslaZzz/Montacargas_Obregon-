package mx.montacargasobregon.backend.equipos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "Captura el nombre")
        @Size(max = 50, message = "El nombre admite máximo 50 caracteres")
        String nombre) {
}
