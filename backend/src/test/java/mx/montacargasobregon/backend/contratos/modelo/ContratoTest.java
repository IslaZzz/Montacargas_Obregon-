package mx.montacargasobregon.backend.contratos.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import mx.montacargasobregon.backend.contratos.ContratoDePrueba;

/** HU-23: el contrato nace Apartado, con anticipo del 30 % y 3 días para pagarlo. */
class ContratoTest {

    private final Contrato contrato = new Contrato("CON-0087", ContratoDePrueba.cotizacion(),
            LocalDateTime.of(2026, 10, 9, 11, 30));

    @Test
    void naceApartadoConElAnticipoDel30PorCiento() {
        assertEquals(EstadoContrato.APARTADO, contrato.getEstado());
        // $9,448.20 × 30 % = $2,834.46, como en el prototipo
        assertEquals(new BigDecimal("2834.46"), contrato.getAnticipo());
    }

    @Test
    void fechaLimiteEsLaAceptacionMasTresDias() {
        assertEquals(LocalDate.of(2026, 10, 12), contrato.getFechaLimiteAnticipo());
    }

    // Inclusivo: el 12 todavía se puede pagar; vence al terminar ese día
    @Test
    void sePuedePagarHastaElDiaDeLaFechaLimite() {
        assertFalse(contrato.plazoDeAnticipoTerminado(LocalDate.of(2026, 10, 11)));
        assertFalse(contrato.plazoDeAnticipoTerminado(LocalDate.of(2026, 10, 12)));
        assertTrue(contrato.plazoDeAnticipoTerminado(LocalDate.of(2026, 10, 13)));
    }

    @Test
    void vencerApartado() {
        contrato.vencerApartado();

        assertEquals(EstadoContrato.APARTADO_VENCIDO, contrato.getEstado());
    }
}
