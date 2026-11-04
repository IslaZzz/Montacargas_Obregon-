package mx.montacargasobregon.backend.comun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 
 * Represente Una dirección recibida desde el front. 
 **/
public record DireccionRequest(
        @NotBlank @Size(max = 100, message = "La calle admite máximo 100 caracteres") String calle,
        @NotBlank @Size(max = 10, message = "El número admite máximo 10 caracteres") String numero,
        @NotBlank @Size(max = 100, message = "La colonia admite máximo 100 caracteres") String colonia,
        @NotBlank @Size(max = 100, message = "La ciudad admite máximo 100 caracteres") String ciudad,
        @NotBlank @Pattern(regexp = "\\d{5}", message = "El código postal debe tener 5 dígitos") String codigoPostal) {
}