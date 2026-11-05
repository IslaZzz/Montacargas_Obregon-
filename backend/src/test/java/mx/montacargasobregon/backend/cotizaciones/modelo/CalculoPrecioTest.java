package mx.montacargasobregon.backend.cotizaciones.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class CalculoPrecioTest {

    @Test
    void del12Al16DeOctubreSon5Dias() {
        assertEquals(5, CalculoPrecio.calcularDias(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16)));
    }

    @Test
    void mismoDiaEs1Dia() {
        assertEquals(1, CalculoPrecio.calcularDias(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1)));
    }

    @Test
    void recoleccionAntesDeEntregaFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculoPrecio.calcularDias(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1)));
    }

    @Test
    void importeEsDiasPorTarifa() {
        assertEquals(new BigDecimal("6000.00"), CalculoPrecio.calcularImporte(5, new BigDecimal("1200")));
    }

    @Test
    void importeRedondeaACentavos() {
        assertEquals(new BigDecimal("2333.31"), CalculoPrecio.calcularImporte(7, new BigDecimal("333.33")));
    }

    @Test
    void ejemploDelFigma() {
        BigDecimal subtotal = CalculoPrecio.calcularSubtotal(List.of(
                CalculoPrecio.calcularImporte(5, new BigDecimal("1200")),
                CalculoPrecio.calcularImporte(5, new BigDecimal("250"))));
        BigDecimal iva = CalculoPrecio.calcularIva(subtotal);

        assertEquals(new BigDecimal("7250.00"), subtotal);
        assertEquals(new BigDecimal("1160.00"), iva);
        assertEquals(new BigDecimal("8410.00"), CalculoPrecio.calcularTotal(subtotal, iva));
    }

    @Test
    void sinEquiposElSubtotalEsCero() {
        assertEquals(new BigDecimal("0.00"), CalculoPrecio.calcularSubtotal(List.of()));
    }
}
