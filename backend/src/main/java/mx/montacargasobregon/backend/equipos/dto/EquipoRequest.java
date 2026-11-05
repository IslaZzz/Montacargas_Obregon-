package mx.montacargasobregon.backend.equipos.dto;

import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;

/** Equipo DTO de solicitud que el backend recibe del front. */
public record EquipoRequest(

        // Obligatorio, hasta 50 caracteres, letras, dígitos y guiones
        @NotBlank @Size(max = 50, message = "El número de unidad admite máximo 50 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El número de unidad solo admite letras, dígitos y guiones")
        String numeroUnidad,

        // Obligatorio, con las mismas reglas de formato
        @NotBlank @Size(max = 50, message = "El número de serie admite máximo 50 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El número de serie solo admite letras, dígitos y guiones")
        String numeroSerie,

        // Obligatorio, no puede ser anterior a 1950
        @NotNull @Min(value = 1950, message = "El año de compra no es válido") Integer anioCompra,

        // Obligatorio: id del modelo al que pertenece la unidad
        @NotNull(message = "El modelo es obligatorio") Long modeloId,

        // Opcional: puede venir vacío o no venir
        Set<Etiqueta> etiquetas) {
}