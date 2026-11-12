package mx.montacargasobregon.backend.cotizaciones.modelo;

import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.ACEPTADA;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.BORRADOR;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.EN_AUTORIZACION;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.ENVIADA;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.NO_ACEPTADA;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.RECHAZADA;
import static mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion.VENCIDA;
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

    @Test
    void enAutorizacionRechazadaYVencidaTodaviaNoCambian() {
        // Sus cambios llegan con HU-16, HU-32 y HU-17
        assertSoloPuedePasarA(EN_AUTORIZACION, Set.of());
        assertSoloPuedePasarA(RECHAZADA, Set.of());
        assertSoloPuedePasarA(VENCIDA, Set.of());
    }

    private void assertSoloPuedePasarA(EstadoCotizacion origen, Set<EstadoCotizacion> permitidos) {
        for (EstadoCotizacion destino : EstadoCotizacion.values()) {
            assertEquals(permitidos.contains(destino), origen.puedeCambiarA(destino),
                    origen + " → " + destino);
        }
    }
}
