package mx.montacargasobregon.backend.movil.traslados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

@ExtendWith(MockitoExtension.class)
class TareasTrasladoServiceTest {

    private static final LocalDateTime DESDE = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2026, 11, 16, 12, 0);

    @Mock
    private TrasladoRepository trasladoRepositorio;

    @Mock
    private CartaResponsivaRepository cartaRepositorio;

    @Mock
    private AvanceRentaService avanceRenta;

    private TareasTrasladoService servicio;

    private final Personal operador = personal(7L, "OP-01");
    private final PersonalActual actual = PersonalActual.de(operador);

    private Renta renta;
    private EquipoRenta mc030;
    private EquipoRenta mc031;
    private Traslado entrega;

    @BeforeEach
    void setUp() {
        servicio = new TareasTrasladoService(trasladoRepositorio, cartaRepositorio, avanceRenta);

        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        renta = RentaDePrueba.nueva("R-2091", cliente, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 26),
                "Calle Sufragio Efectivo 300", "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(renta, "id", 90L);
        mc030 = renta.agregarEquipo(new Equipo("MC-030", "SN-030", 2021, null));
        mc031 = renta.agregarEquipo(new Equipo("MC-031", "SN-031", 2021, null));

        entrega = traslado(50L, TipoTraslado.ENTREGA);
        entrega.asignarResponsable(operador);
    }

    @Test
    void misTareasSoloTraeLasDelTipoPedidoQueSiguenPendientes() {
        Traslado recoleccion = traslado(51L, TipoTraslado.RECOLECCION);
        Traslado realizada = traslado(52L, TipoTraslado.ENTREGA);
        realizada.completar();
        when(trasladoRepositorio.buscarPorOperador(7L)).thenReturn(List.of(entrega, recoleccion, realizada));
        when(cartaRepositorio.findByRentaIdIn(Set.of(90L))).thenReturn(List.of());

        List<TareaResponse> tareas = servicio.misTareas(actual, TipoTraslado.ENTREGA);

        assertEquals(1, tareas.size());
        assertEquals("TR-0050", tareas.get(0).folio());
        assertEquals("Industrias Cajeme", tareas.get(0).cliente());
        assertEquals(2, tareas.get(0).equipos());
        assertFalse(tareas.get(0).firmaEnSitio());
    }

    @Test
    void elDetalleDiceQueFaltanEquiposPorPreparar() {
        mc030.cambiarFase(FaseEquipoRenta.LISTO_ENTREGA);
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.empty());

        DetalleTareaResponse detalle = servicio.detalle(50L, actual);

        assertEquals(1, detalle.listos());
        assertFalse(detalle.puedeIniciar());
        assertNotNull(detalle.bloqueo());
        assertTrue(detalle.bloqueo().contains("MC-031"));
    }

    @Test
    void iniciarDejaLosEquiposEnTrasladoCuandoTodosEstanListos() {
        mc030.cambiarFase(FaseEquipoRenta.LISTO_ENTREGA);
        mc031.cambiarFase(FaseEquipoRenta.LISTO_ENTREGA);
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.empty());

        DetalleTareaResponse detalle = servicio.iniciar(50L, actual);

        assertEquals(EstadoTraslado.EN_CURSO, entrega.getEstado());
        assertTrue(detalle.puedeCompletar());
        assertFalse(detalle.puedeIniciar());
        verify(avanceRenta).trasladoIniciado(entrega);
    }

    @Test
    void noSePuedeIniciarSiFaltaPrepararUnEquipo() {
        mc030.cambiarFase(FaseEquipoRenta.LISTO_ENTREGA);
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.iniciar(50L, actual));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(EstadoTraslado.PROGRAMADO, entrega.getEstado());
        verifyNoInteractions(avanceRenta);
    }

    @Test
    void completarPideQueElTrasladoYaHayaIniciado() {
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.completar(50L, actual));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(avanceRenta);
    }

    @Test
    void completarMarcaElTrasladoYHaceAvanzarLaRenta() {
        entrega.iniciar();
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.empty());

        servicio.completar(50L, actual);

        assertEquals(EstadoTraslado.COMPLETADO, entrega.getEstado());
        verify(avanceRenta).trasladoRealizado(entrega);
    }

    @Test
    void noSePuedeAbrirUnaTareaDeOtroOperador() {
        PersonalActual otro = PersonalActual.de(personal(8L, "OP-02"));
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.detalle(50L, otro));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    private Traslado traslado(Long id, TipoTraslado tipo) {
        Traslado t = new Traslado(String.format("TR-%04d", id), renta, tipo, DESDE, HASTA);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private static Personal personal(Long id, String codigo) {
        Personal p = new Personal(codigo, "Rafael", "Amaya", "López", TipoPersonal.OP_TRASLADO);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }
}