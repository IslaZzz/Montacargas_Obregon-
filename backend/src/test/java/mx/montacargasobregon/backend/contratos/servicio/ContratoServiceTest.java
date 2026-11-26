package mx.montacargasobregon.backend.contratos.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.montacargasobregon.backend.contratos.ContratoDePrueba;
import mx.montacargasobregon.backend.contratos.modelo.Contrato;
import mx.montacargasobregon.backend.contratos.modelo.EstadoContrato;
import mx.montacargasobregon.backend.contratos.repositorio.ContratoRepository;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;

/** HU-23: generar el contrato, vencer el apartado y avisar en otras cotizaciones. */
@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock
    private ContratoRepository repositorio;

    private ContratoService servicio;
    private Cotizacion cotizacion;

    @BeforeEach
    void setUp() {
        servicio = new ContratoService(repositorio);
        cotizacion = ContratoDePrueba.cotizacion();
    }

    @Test
    void elPrimerContratoEsCON0001() {
        when(repositorio.findByCotizacionId(158L)).thenReturn(Optional.empty());
        when(repositorio.ultimoNumeroFolio()).thenReturn(null);
        when(repositorio.save(any(Contrato.class))).thenAnswer(i -> i.getArgument(0));

        Contrato contrato = servicio.generar(cotizacion);

        assertEquals("CON-0001", contrato.getFolio());
        assertEquals(EstadoContrato.APARTADO, contrato.getEstado());
        assertSame(cotizacion, contrato.getCotizacion());
    }

    @Test
    void sigueLaSerieDeFolios() {
        when(repositorio.findByCotizacionId(158L)).thenReturn(Optional.empty());
        when(repositorio.ultimoNumeroFolio()).thenReturn(87);
        when(repositorio.save(any(Contrato.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("CON-0088", servicio.generar(cotizacion).getFolio());
    }

    @Test
    void unaCotizacionGeneraUnSoloContrato() {
        Contrato existente = new Contrato("CON-0087", cotizacion, LocalDateTime.of(2026, 10, 9, 11, 30));
        when(repositorio.findByCotizacionId(158L)).thenReturn(Optional.of(existente));

        assertSame(existente, servicio.generar(cotizacion));
        verify(repositorio, never()).save(any());
    }

    // Con la fecha de hoy en Sonora: el que tuvo su fecha límite ayer vence; el de hoy todavía se puede pagar
    @Test
    void venceAlTerminarElDiaDeLaFechaLimite() {
        LocalDateTime ahora = LocalDateTime.now(Cotizacion.ZONA_HORARIA);
        Contrato limiteAyer = new Contrato("CON-0086", cotizacion, ahora.minusDays(4));
        Contrato limiteHoy = new Contrato("CON-0087", cotizacion, ahora.minusDays(3));
        when(repositorio.findByEstado(EstadoContrato.APARTADO)).thenReturn(List.of(limiteAyer, limiteHoy));

        assertEquals(1, servicio.vencerApartados());
        assertEquals(EstadoContrato.APARTADO_VENCIDO, limiteAyer.getEstado());
        assertEquals(EstadoContrato.APARTADO, limiteHoy.getEstado());
    }

    // Busca por días completos: del 12 de octubre a las 00:00 al 17 a las 00:00
    @Test
    void avisaDeLosEquiposApartadosEnLasMismasFechas() {
        cotizacion.cambiarEstado(EstadoCotizacion.ENVIADA, LocalDateTime.of(2026, 10, 5, 10, 0));
        when(repositorio.equiposEnContratoDeOtraCotizacion(EstadoContrato.APARTADO, 158L, List.of(7L, 5L),
                LocalDateTime.of(2026, 10, 12, 0, 0), LocalDateTime.of(2026, 10, 17, 0, 0)))
                .thenReturn(List.of(7L));

        assertEquals(Set.of(7L), servicio.equiposEnOtraCotizacionAceptada(cotizacion));
    }

    @Test
    void unaCotizacionYaRespondidaNoAvisa() {
        cotizacion.cambiarEstado(EstadoCotizacion.ACEPTADA, LocalDateTime.of(2026, 10, 9, 11, 30));

        assertTrue(servicio.equiposEnOtraCotizacionAceptada(cotizacion).isEmpty());
        verifyNoInteractions(repositorio);
    }
}
