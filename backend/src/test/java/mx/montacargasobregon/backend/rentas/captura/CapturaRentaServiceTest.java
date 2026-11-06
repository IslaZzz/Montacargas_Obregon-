package mx.montacargasobregon.backend.rentas.captura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.repositorio.ClienteRepository;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.repositorio.EquipoRepository;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;
import mx.montacargasobregon.backend.revisiones.repositorio.RevisionRepository;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;

@ExtendWith(MockitoExtension.class)
class CapturaRentaServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 10);
    private static final LocalDate FIN = LocalDate.of(2026, 11, 20);
    private static final LocalDateTime ENTREGA_DESDE = LocalDateTime.of(2026, 11, 10, 8, 0);
    private static final LocalDateTime ENTREGA_HASTA = LocalDateTime.of(2026, 11, 10, 10, 0);

    @Mock
    private CapturaRentaRepository repositorio;

    @Mock
    private ClienteRepository clienteRepositorio;

    @Mock
    private EquipoRepository equipoRepositorio;

    @Mock
    private RevisionRepository revisionRepositorio;

    @Mock
    private AvanceRentaService avanceRenta;

    private CapturaRentaService servicio;

    private Cliente cliente;
    private Modelo modelo;
    private Equipo mc030;
    private Equipo mc031;

    @BeforeEach
    void setUp() {
        servicio = new CapturaRentaService(repositorio, clienteRepositorio, equipoRepositorio,
                revisionRepositorio, avanceRenta);

        cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(cliente, "id", 1L);

        modelo = new Modelo("8FGU25", "Toyota", "TOY-8FGU25", null, new BigDecimal("1500"),
                new Categoria("Montacargas"));

        mc030 = equipo(30L, "MC-030");
        mc031 = equipo(31L, "MC-031");
    }

    private Equipo equipo(Long id, String numeroUnidad) {
        Equipo e = new Equipo(numeroUnidad, "SN-" + numeroUnidad, 2021, modelo);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private CrearRentaRequest solicitud(List<Long> equipoIds, LocalDateTime desde, LocalDateTime hasta) {
        return new CrearRentaRequest(1L, INICIO, FIN, "Calle Sufragio Efectivo 300",
                "Ramón Gutiérrez", "6441000001", equipoIds, desde, hasta);
    }

    // Renta ya guardada con MC-030 y MC-031, para confirmar su entrega
    private Renta rentaGuardada() {
        Renta renta = new Renta("R-2091", cliente, INICIO, FIN, "Calle Sufragio Efectivo 300",
                "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(renta, "id", 20L);
        renta.agregarEquipo(mc030);
        renta.agregarEquipo(mc031);
        return renta;
    }

    // Lo que necesita crear() para llegar a guardar
    private void prepararCreacion(Integer ultimoFolio) {
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipoRepositorio.findAllById(Set.of(30L, 31L))).thenReturn(List.of(mc030, mc031));
        when(repositorio.equiposOcupados(eq(INICIO), eq(FIN), any(), any()))
                .thenReturn(List.of());
        when(repositorio.ultimoNumeroFolio()).thenReturn(ultimoFolio);
        when(repositorio.save(any(Renta.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crear_conEntrega_creaRentaYRevisionesDeSalida() {
        prepararCreacion(2090);

        RentaCapturadaResponse renta = servicio.crear(solicitud(List.of(30L, 31L), ENTREGA_DESDE, ENTREGA_HASTA));

        assertEquals("R-2091", renta.folio());
        assertEquals("Industrias Cajeme", renta.cliente());
        assertEquals("PROGRAMADA", renta.estado());
        assertEquals(List.of("MC-030", "MC-031"), renta.equipos());
        assertEquals(ENTREGA_DESDE, renta.entregaDesde());

        // Una revisión de salida pendiente por equipo
        ArgumentCaptor<Revision> revisiones = ArgumentCaptor.forClass(Revision.class);
        verify(revisionRepositorio, times(2)).save(revisiones.capture());
        for (Revision r : revisiones.getAllValues()) {
            assertEquals(TipoRevision.SALIDA, r.getTipo());
            assertEquals(EstadoRevision.PENDIENTE, r.getEstado());
            assertEquals(ENTREGA_DESDE, r.getFechaProgramada());
        }
    }

    @Test
    void crear_sinEntrega_noCreaRevisiones() {
        prepararCreacion(2090);

        RentaCapturadaResponse renta = servicio.crear(solicitud(List.of(30L, 31L), null, null));

        assertEquals("R-2091", renta.folio());
        assertEquals(null, renta.entregaDesde());
        verifyNoInteractions(revisionRepositorio);
    }

    @Test
    void crear_usaElPrimerFolio_cuandoNoHayRentas() {
        prepararCreacion(null);

        RentaCapturadaResponse renta = servicio.crear(solicitud(List.of(30L, 31L), null, null));

        assertEquals("R-0001", renta.folio());
    }

    @Test
    void crear_rechazaConConflicto_cuandoUnEquipoEstaEnOtraRenta() {
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipoRepositorio.findAllById(Set.of(30L, 31L))).thenReturn(List.of(mc030, mc031));
        when(repositorio.equiposOcupados(eq(INICIO), eq(FIN), any(), any()))
                .thenReturn(List.of(31L));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(List.of(30L, 31L), null, null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("MC-031"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_rechazaConConflicto_cuandoUnEquipoNoEstaOperativo() {
        ReflectionTestUtils.setField(mc031, "estado", EstadoEquipo.FUERA_SERVICIO);
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipoRepositorio.findAllById(Set.of(30L, 31L))).thenReturn(List.of(mc030, mc031));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(List.of(30L, 31L), null, null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("MC-031"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_responde404_cuandoElClienteNoExiste() {
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(List.of(30L), null, null)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(equipoRepositorio);
    }

    @Test
    void crear_responde404_cuandoUnEquipoNoExiste() {
        when(clienteRepositorio.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipoRepositorio.findAllById(Set.of(30L, 99L))).thenReturn(List.of(mc030));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(List.of(30L, 99L), null, null)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void crear_responde400_cuandoElFinEsAntesDelInicio() {
        CrearRentaRequest alReves = new CrearRentaRequest(1L, FIN, INICIO, "Calle Sufragio Efectivo 300",
                "Ramón Gutiérrez", "6441000001", List.of(30L), null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(alReves));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(clienteRepositorio);
    }

    @Test
    void crear_responde400_cuandoSoloVieneElInicioDeLaEntrega() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(List.of(30L), ENTREGA_DESDE, null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(clienteRepositorio);
    }

    @Test
    void crear_responde400_cuandoLaEntregaTerminaAntesDeEmpezar() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(List.of(30L), ENTREGA_HASTA, ENTREGA_DESDE)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void confirmarEntrega_creaRevisionesDeSalida() {
        Renta renta = rentaGuardada();
        when(repositorio.findById(20L)).thenReturn(Optional.of(renta));

        RentaCapturadaResponse respuesta = servicio.confirmarEntrega(20L, new HorarioRequest(ENTREGA_DESDE, ENTREGA_HASTA));

        assertEquals(ENTREGA_DESDE, respuesta.entregaDesde());
        assertEquals(ENTREGA_HASTA, respuesta.entregaHasta());
        verify(revisionRepositorio, times(2)).save(any(Revision.class));
    }

    @Test
    void confirmarEntrega_rechazaConConflicto_cuandoYaTieneHorario() {
        Renta renta = rentaGuardada();
        renta.confirmarEntrega(ENTREGA_DESDE, ENTREGA_HASTA);
        when(repositorio.findById(20L)).thenReturn(Optional.of(renta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarEntrega(20L, new HorarioRequest(ENTREGA_DESDE, ENTREGA_HASTA)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("R-2091"));
        verifyNoInteractions(revisionRepositorio);
    }

    @Test
    void confirmarEntrega_rechazaConConflicto_cuandoLaRentaNoEstaProgramada() {
        Renta renta = rentaGuardada();
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        when(repositorio.findById(20L)).thenReturn(Optional.of(renta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarEntrega(20L, new HorarioRequest(ENTREGA_DESDE, ENTREGA_HASTA)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void confirmarEntrega_responde404_cuandoLaRentaNoExiste() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarEntrega(99L, new HorarioRequest(ENTREGA_DESDE, ENTREGA_HASTA)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void equiposDisponibles_soloDevuelveOperativosLibresOrdenados() {
        Equipo descompuesto = equipo(32L, "MC-032");
        ReflectionTestUtils.setField(descompuesto, "estado", EstadoEquipo.FUERA_SERVICIO);
        Equipo ocupado = equipo(33L, "MC-033");

        when(repositorio.equiposOcupados(eq(INICIO), eq(FIN), any(), any()))
                .thenReturn(List.of(33L));
        when(equipoRepositorio.findAll()).thenReturn(List.of(mc031, ocupado, descompuesto, mc030));

        List<EquipoDisponibleResponse> disponibles = servicio.equiposDisponibles(INICIO, FIN);

        assertEquals(List.of("MC-030", "MC-031"),
                disponibles.stream().map(EquipoDisponibleResponse::numeroUnidad).toList());
        assertEquals("Montacargas", disponibles.get(0).categoria());
        assertEquals("Toyota 8FGU25", disponibles.get(0).descripcion());
        // Un equipo que ya regresó de su renta cuenta como libre
        verify(repositorio).equiposOcupados(INICIO, FIN,
                List.of(EstadoRenta.PROGRAMADA, EstadoRenta.EN_CURSO),
                List.of(FaseEquipoRenta.CANCELADO, FaseEquipoRenta.COMPLETADO));
    }

    @Test
    void rentasSinRecoleccion_soloPideLasRentasEnCurso() {
        Renta renta = rentaGuardada();
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        when(repositorio.findByEstadoAndRecoleccionDesdeIsNullOrderByFechaFinAsc(EstadoRenta.EN_CURSO))
                .thenReturn(List.of(renta));

        List<RentaCapturadaResponse> rentas = servicio.rentasSinRecoleccion();

        assertEquals(List.of("R-2091"), rentas.stream().map(RentaCapturadaResponse::folio).toList());
    }

    @Test
    void confirmarRecoleccion_programaElTrasladoDeRecoleccion() {
        Renta renta = rentaGuardada();
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        when(repositorio.findById(20L)).thenReturn(Optional.of(renta));
        LocalDateTime desde = LocalDateTime.of(2026, 11, 20, 14, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 11, 20, 16, 0);

        servicio.confirmarRecoleccion(20L, new HorarioRequest(desde, hasta));

        verify(avanceRenta).programarRecoleccion(renta, desde, hasta);
    }

    @Test
    void confirmarRecoleccion_rechazaConConflicto_cuandoLaRentaNoEstaEnCurso() {
        Renta renta = rentaGuardada();
        when(repositorio.findById(20L)).thenReturn(Optional.of(renta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarRecoleccion(20L, new HorarioRequest(
                        LocalDateTime.of(2026, 11, 20, 14, 0), LocalDateTime.of(2026, 11, 20, 16, 0))));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(avanceRenta);
    }

    @Test
    void confirmarRecoleccion_rechazaConConflicto_cuandoYaTieneHorario() {
        Renta renta = rentaGuardada();
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        renta.confirmarRecoleccion(LocalDateTime.of(2026, 11, 20, 14, 0), LocalDateTime.of(2026, 11, 20, 16, 0));
        when(repositorio.findById(20L)).thenReturn(Optional.of(renta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarRecoleccion(20L, new HorarioRequest(
                        LocalDateTime.of(2026, 11, 21, 8, 0), LocalDateTime.of(2026, 11, 21, 10, 0))));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("R-2091"));
        verifyNoInteractions(avanceRenta);
    }

    @Test
    void confirmarRecoleccion_rechaza_cuandoElHorarioTerminaAntesDeEmpezar() {
        Renta renta = rentaGuardada();
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        when(repositorio.findById(20L)).thenReturn(Optional.of(renta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarRecoleccion(20L, new HorarioRequest(
                        LocalDateTime.of(2026, 11, 20, 16, 0), LocalDateTime.of(2026, 11, 20, 14, 0))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(avanceRenta);
    }
}