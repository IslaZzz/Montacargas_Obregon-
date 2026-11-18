package mx.montacargasobregon.backend.equipos.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario de modelo (HU-36).
 * marcaModelo es un solo texto, como "Toyota 8FBE30": la primera palabra es la marca y el resto el código.
 * especificaciones lleva el valor de cada campo de la categoría, por el id del campo.
 */
public record ModeloRequest(
        @NotNull(message = "Este dato es obligatorio")
        Long categoriaId,

        @NotBlank(message = "Este dato es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "Este dato es obligatorio")
        @Size(max = 150, message = "Admite máximo 150 caracteres")
        String marcaModelo,

        @Size(max = 120, message = "La descripción admite máximo 120 caracteres")
        String descripcion,

        Map<Long, String> especificaciones) {
}
