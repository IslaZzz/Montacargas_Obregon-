package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PATCH /api/cotizaciones/5/traslado: lo que cuesta llevar y recoger el equipo. */
public record TrasladoCotizacionRequest(
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal trasladoEntrega,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal trasladoRecoleccion) {
}