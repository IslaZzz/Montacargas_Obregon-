package mx.montacargasobregon.backend.equipos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Tarifa DTO de solicitud que el backend recibe del front. */
public record TarifaRequest(
        // Obligatoria y mayor a cero
        @NotNull(message = "Captura una tarifa mayor a cero")
        @DecimalMin(value = "0.01", message = "Captura una tarifa mayor a cero")
        // Igual que la columna tarifa_dia, DECIMAL(10,2): hasta 8 enteros y 2 decimales
        @Digits(integer = 8, fraction = 2, message = "La tarifa admite máximo 8 enteros y 2 decimales")
        BigDecimal tarifaDia) {
}