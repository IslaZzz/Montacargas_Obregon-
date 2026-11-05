package mx.montacargasobregon.backend.cotizaciones.modelo;

import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.ACEPTADA;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.BORRADOR;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.ENVIADA;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.NO_ACEPTADA;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** Revisa todas las combinaciones de cambio de estado. */
class EstadoCotizacionTest {

    @Test
    void borradorSoloPasaAEnviada() {
        assertSoloPuedePasarA(BORRADOR, Set.of(ENVIADA));
    }

    @Test
    void enviadaPasaAAceptadaONoAceptada() {
        assertSoloPuedePasarA(ENVIADA, Set.of(ACEPTADA, NO_ACEPTADA));
    }

    @Test
    void aceptadaYNoAceptadaSonFinales() {
        assertSoloPuedePasarA(ACEPTADA, Set.of());
        assertSoloPuedePasarA(NO_ACEPTADA, Set.of());
    }

    private void assertSoloPuedePasarA(EstadoCotizacion origen, Set<EstadoCotizacion> permitidos) {
        for (EstadoCotizacion destino : EstadoCotizacion.values()) {
            assertEquals(permitidos.contains(destino), origen.puedeCambiarA(destino),
                    origen + " → " + destino);
        }
    }
}
