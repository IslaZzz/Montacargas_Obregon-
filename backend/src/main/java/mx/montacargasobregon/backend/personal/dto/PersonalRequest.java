package mx.montacargasobregon.backend.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;

/** Personal DTO de solicitud que el backend recibe del front. */
public record PersonalRequest(

        // Obligatorio el código con el que se le identifica
        @NotBlank @Size(max = 20, message = "El código admite máximo 20 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El código solo admite letras, dígitos y guiones")
        String codigo,

        @NotBlank @Size(max = 50, message = "Los nombres admiten máximo 50 caracteres")
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Los nombres solo admiten letras")
        String nombres,

        @NotBlank @Size(max = 50, message = "El apellido paterno admite máximo 50 caracteres")
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "El apellido paterno solo admite letras")
        String apellidoPaterno,

        // Opcional, puede no enviarse
        @Size(max = 50, message = "El apellido materno admite máximo 50 caracteres")
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "El apellido materno solo admite letras")
        String apellidoMaterno,

        @NotNull(message = "El tipo es obligatorio") TipoPersonal tipo) {
}