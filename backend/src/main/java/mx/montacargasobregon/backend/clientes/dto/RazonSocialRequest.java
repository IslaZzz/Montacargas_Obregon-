package mx.montacargasobregon.backend.clientes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.montacargasobregon.backend.comun.dto.DireccionRequest;

/**
 * Representa una razón social recibida dentro del registro de cliente.
 */
public record RazonSocialRequest(
        @NotBlank @Size(max = 150, message = "El nombre admite máximo 150 caracteres") String nombre,

        // 3 o 4 letras, 6 dígitos de fecha y 3 de homoclave (12 o 13 caracteres)
        @NotBlank @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "El RFC debe tener 12 o 13 caracteres con formato válido") String rfc,

        // Obligatoria, @Valid aplica las reglas de DireccionRequest
        @NotNull @Valid DireccionRequest direccionFiscal) {
}