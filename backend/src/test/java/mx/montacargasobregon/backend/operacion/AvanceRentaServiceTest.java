package mx.montacargasobregon.backend.operacion.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

@ExtendWith(MockitoExtension.class)
class AvanceRentaServiceTest {

    private static final LocalDateTime ENTREGA_DESDE = LocalDateTime.of(2026, 11, 10, 8, 0);
    private static final LocalDateTime ENTREGA_HASTA = LocalDateTime.of(2026, 11, 10, 10, 0);

    @Mock
    private TrasladoRepository trasladoRepositorio;

    private AvanceRentaService servicio;

    private Renta renta;
    private EquipoRenta mc030;
    private EquipoRenta mc031;

    @BeforeEach
    void setUp() {
        servicio = new AvanceRentaService(trasladoRepositorio);

        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        renta = new Renta("R-2091", cliente, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 20),
                "Calle Sufragio Efectivo 300", "Ramón Gutiérrez", "6441000001");
        renta.confirmarEntrega(ENTREGA_DESDE, ENTREGA_HASTA);
        mc030 = renta.agregarEquipo(new Equipo("MC-030", "SN-030", 2021, null));
        mc031 = renta.agregarEquipo(new Equipo("MC-031", "SN-031", 2021, null));
    }

    @Test
    void equipoListoParaEntrega_creaElTraslado_cuandoTodosEstanListos() {
        mc031.cambiarFase(FaseEquipoRenta.LISTO_ENTREGA);
        when(trasladoRepositorio.existsByRentaAndTipoAndEstadoNot(renta, TipoTraslado.ENTREGA, EstadoTraslado.CANCELADO))
                .thenReturn(false);
        when(trasladoRepositorio.ultimoNumeroFolio()).thenReturn(86);

        servicio.equipoListoParaEntrega(mc030);

        assertEquals(FaseEquipoRenta.LISTO_ENTREGA, mc030.getFase());
        ArgumentCaptor<Traslado> traslado = ArgumentCaptor.forClass(Traslado.class);
        verify(trasladoRepositorio).save(traslado.capture());
        assertEquals("TR-0087", traslado.getValue().getFolio());
        assertSame(renta, traslado.getValue().getRenta());
        assertEquals(TipoTraslado.ENTREGA, traslado.getValue().getTipo());
        assertEquals(EstadoTraslado.PROGRAMADO, traslado.getValue().getEstado());
        assertEquals(ENTREGA_DESDE, traslado.getValue().getFechaHoraInicio());
        assertEquals(ENTREGA_HASTA, traslado.getValue().getFechaHoraFin());
    }

    @Test
    void equipoListoParaEntrega_noCreaElTraslado_mientrasFaltenEquipos() {
        servicio.equipoListoParaEntrega(mc030);

        assertEquals(FaseEquipoRenta.LISTO_ENTREGA, mc030.getFase());
        verify(trasladoRepositorio, never()).save(any());
    }

    @Test
    void equipoListoParaEntrega_noRepiteElTraslado_cuandoYaExiste() {
        mc031.cambiarFase(FaseEquipoRenta.LISTO_ENTREGA);
        when(trasladoRepositorio.existsByRentaAndTipoAndEstadoNot(renta, TipoTraslado.ENTREGA, EstadoTraslado.CANCELADO))
                .thenReturn(true);

        servicio.equipoListoParaEntrega(mc030);

        verify(trasladoRepositorio, never()).save(any());
    }

    @Test
    void equipoListoParaEntrega_noEsperaALosEquiposCancelados() {
        mc031.cambiarFase(FaseEquipoRenta.CANCELADO);
        when(trasladoRepositorio.existsByRentaAndTipoAndEstadoNot(renta, TipoTraslado.ENTREGA, EstadoTraslado.CANCELADO))
                .thenReturn(false);
        when(trasladoRepositorio.ultimoNumeroFolio()).thenReturn(null);

        servicio.equipoListoParaEntrega(mc030);

        ArgumentCaptor<Traslado> traslado = ArgumentCaptor.forClass(Traslado.class);
        verify(trasladoRepositorio).save(traslado.capture());
        assertEquals("TR-0001", traslado.getValue().getFolio());
    }

    @Test
    void equipoDevuelto_cierraLaRenta_cuandoRegresanTodos() {
        mc031.cambiarFase(FaseEquipoRenta.COMPLETADO);

        servicio.equipoDevuelto(mc030);

        assertEquals(FaseEquipoRenta.COMPLETADO, mc030.getFase());
        assertEquals(EstadoRenta.CERRADA, renta.getEstado());
        verifyNoInteractions(trasladoRepositorio);
    }

    @Test
    void equipoDevuelto_noCierraLaRenta_mientrasFaltenEquipos() {
        mc031.cambiarFase(FaseEquipoRenta.EN_OPERACION);

        servicio.equipoDevuelto(mc030);

        assertEquals(FaseEquipoRenta.COMPLETADO, mc030.getFase());
        assertEquals(EstadoRenta.PROGRAMADA, renta.getEstado());
    }
}