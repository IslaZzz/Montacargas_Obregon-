package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class CalculoPrecio {

    public static final BigDecimal TASA_IVA = new BigDecimal("0.16");

    private CalculoPrecio() {
    }

    public static int calcularDias(LocalDate entrega, LocalDate recoleccion) {
        if (recoleccion.isBefore(entrega)) {
            throw new IllegalArgumentException("La recolección no puede ser antes de la entrega");
        }
        return (int) ChronoUnit.DAYS.between(entrega, recoleccion) + 1;
    }

    public static BigDecimal calcularImporte(int dias, BigDecimal tarifaPorDia) {
        return tarifaPorDia.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcularSubtotal(List<BigDecimal> importes) {
        return importes.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcularIva(BigDecimal subtotal) {
        return subtotal.multiply(TASA_IVA).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcularTotal(BigDecimal subtotal, BigDecimal iva) {
        return subtotal.add(iva);
    }
}
