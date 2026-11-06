package mx.montacargasobregon.backend.comun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dirección del lugar de entrega recibida desde el front. Las referencias son opcionales. */
public record DatosDireccionRequest(
        @NotBlank(message = "Escribe la calle")
        @Size(max = 100, message = "La calle admite máximo 100 caracteres") String calle,
        @NotBlank(message = "Escribe el número")
        @Size(max = 10, message = "El número admite máximo 10 caracteres") String numero,
        @NotBlank(message = "Escribe la colonia")
        @Size(max = 100, message = "La colonia admite máximo 100 caracteres") String colonia,
        @NotBlank(message = "Escribe la ciudad")
        @Size(max = 100, message = "La ciudad admite máximo 100 caracteres") String ciudad,
        @NotBlank(message = "Escribe el estado")
        @Size(max = 50, message = "El estado admite máximo 50 caracteres") String estado,
        @NotBlank(message = "Escribe el código postal")
        @Pattern(regexp = "\\d{5}", message = "El código postal debe tener 5 dígitos") String codigoPostal,
        @Size(max = 200, message = "Las referencias admiten máximo 200 caracteres") String referencias) {
}
