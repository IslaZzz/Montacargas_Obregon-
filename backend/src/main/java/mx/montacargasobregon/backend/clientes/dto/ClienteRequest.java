package mx.montacargasobregon.backend.clientes.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cliente DTO de solicitud que el backend recibe del front.
 * Es un record, Java genera automáticamente el constructor y los métodos de
 * acceso.
 * Las anotaciones definen las reglas. Si alguna falla, Spring responde 400 sin
 * llamar al servicio.
 */
public record ClienteRequest(
        // Obligatorio, hasta 50 caracteres, solo letras (con acentos y ñ), espacios, punto, apóstrofo y guion
        @NotBlank @Size(max = 50, message = "El nombre admite máximo 50 caracteres") @Pattern(regexp = "^[\\p{L} .'-]+$", message = "El nombre solo admite letras") String nombre,

        // Obligatorio, con las mismas reglas de formato
        @NotBlank @Size(max = 50, message = "El apellido paterno admite máximo 50 caracteres") @Pattern(regexp = "^[\\p{L} .'-]+$", message = "El apellido paterno solo admite letras") String apellidoPaterno,

        // Opcional: puede no enviarse (null). Si se envía, debe cumplir el formato
        @Size(max = 50, message = "El apellido materno admite máximo 50 caracteres") @Pattern(regexp = "^[\\p{L} .'-]+$", message = "El apellido materno solo admite letras") String apellidoMaterno,

        // Hasta 100 caracteres
        @Size(max = 100, message = "La empresa admite máximo 100 caracteres") String empresa,

        // Obligatorio y con exactamente 10 dígitos
        @NotBlank @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos") String telefono,

        // Obligatorio, debe ser un correo válido, hasta 150 caracteres
        @NotBlank @Email @Size(max = 150, message = "El correo admite máximo 150 caracteres") String correo,

        // Obligatorio, no puede estar vacío ni nulo, y cada elemento debe ser válido
        @NotEmpty @Valid List<RazonSocialRequest> razonesSociales) {
}
