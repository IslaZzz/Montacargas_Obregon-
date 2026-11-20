package mx.montacargasobregon.backend.equipos.dto;

import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del formulario «Registrar equipo» (HU-02). Estado, ubicación y horas de uso los maneja Operativo. */
public record EquipoRequest(

        // Obligatorio, hasta 50 caracteres, letras, dígitos y guiones
        @NotBlank(message = "Este dato es obligatorio")
        @Size(max = 50, message = "El número de unidad admite máximo 50 caracteres")
        @Pattern(regexp = "^\\s*[A-Za-z0-9-]*\\s*$", message = "El número de unidad solo admite letras, dígitos y guiones")
        String numeroUnidad,

        // Obligatorio, con las mismas reglas de formato
        @NotBlank(message = "Este dato es obligatorio")
        @Size(max = 50, message = "El número de serie admite máximo 50 caracteres")
        @Pattern(regexp = "^\\s*[A-Za-z0-9-]*\\s*$", message = "El número de serie solo admite letras, dígitos y guiones")
        String numeroSerie,

        // Opcional: año de compra o fabricación, de 4 dígitos. La pantalla además no deja pasar un año futuro
        @Min(value = 1950, message = "Escribe un año de 4 dígitos, de 1950 en adelante")
        @Max(value = 2100, message = "Escribe un año de 4 dígitos, de 1950 en adelante")
        Integer anioCompra,

        // Obligatorio: id del modelo al que pertenece la unidad
        @NotNull(message = "Este dato es obligatorio") Long modeloId,

        // Opcional: ids de sus etiquetas; puede venir vacío o no venir
        Set<Long> etiquetaIds) {
}
