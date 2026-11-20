package mx.montacargasobregon.backend.equipos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nombre de la etiqueta que se agrega con «+ Nueva etiqueta» (HU-02). */
public record EtiquetaRequest(
        @NotBlank(message = "Este dato es obligatorio")
        @Size(max = 50, message = "La etiqueta admite máximo 50 caracteres")
        String nombre) {
}
