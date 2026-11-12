package mx.montacargasobregon.backend.rentas.captura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
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
        servicio = new CapturaRentaService(repositorio, revisionRepositorio, avanceRenta);

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

    // Renta ya guardada con MC-030 y MC-031, para confirmar su entrega
    private Renta rentaGuardada() {
        Renta renta = RentaDePrueba.nueva("R-2091", cliente, INICIO, FIN, "Calle Sufragio Efectivo 300",
                "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(renta, "id", 20L);
        renta.agregarEquipo(mc030);
        renta.agregarEquipo(mc031);
        return renta;
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