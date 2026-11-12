package mx.montacargasobregon.backend.rentas.detalle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;
import mx.montacargasobregon.backend.traslados.modelo.EstadoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.TipoTraslado;
import mx.montacargasobregon.backend.traslados.modelo.Traslado;
import mx.montacargasobregon.backend.traslados.repositorio.TrasladoRepository;

@ExtendWith(MockitoExtension.class)
class DetalleRentaServiceTest {

    // Fechas lejanas para que nunca queden en el pasado
    private static final LocalDateTime DESDE = LocalDateTime.of(2030, 10, 9, 9, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2030, 10, 9, 11, 0);

    @Mock
    private RentaRepository rentaRepositorio;

    @Mock
    private TrasladoRepository trasladoRepositorio;

    @Mock
    private AvanceRentaService avanceRenta;

    private DetalleRentaService servicio;

    private Renta renta;
    private Personal operadorLuis;

    @BeforeEach
    void setUp() {
        servicio = new DetalleRentaService(rentaRepositorio, trasladoRepositorio, avanceRenta);

        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        renta = new Renta("R-2091", cliente, LocalDate.of(2030, 9, 23), LocalDate.of(2030, 10, 9),
                "Calle Sufragio Efectivo 300", "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(renta, "id", 20L);

        Modelo modelo = new Modelo("8FGU25", "Toyota", "TOY-8FGU25", null, new BigDecimal("1500"),
                new Categoria("Montacargas"));
        Equipo mc030 = new Equipo("MC-030", "SN-030", 2021, modelo);
        Equipo mc031 = new Equipo("MC-031", "SN-031", 2021, modelo);
        renta.agregarEquipo(mc030).cambiarFase(FaseEquipoRenta.EN_OPERACION);
        renta.agregarEquipo(mc031).cambiarFase(FaseEquipoRenta.CANCELADO);

        operadorLuis = new Personal("OPT-10", "Luis", "Valenzuela", null, TipoPersonal.OP_TRASLADO);
        ReflectionTestUtils.setField(operadorLuis, "id", 100L);
    }

    private Traslado recoleccion() {
        Traslado t = new Traslado("TR-0091", renta, TipoTraslado.RECOLECCION, DESDE, HASTA);
        ReflectionTestUtils.setField(t, "id", 300L);
        return t;
    }

    private void conRecoleccion(Traslado traslado) {
        when(trasladoRepositorio.findFirstByRentaAndTipoAndEstadoNotOrderByIdDesc(
                renta, TipoTraslado.RECOLECCION, EstadoTraslado.CANCELADO))
                .thenReturn(Optional.ofNullable(traslado));
    }

    @Test
    void obtener_devuelveLaRentaConSusEquiposYSuRecoleccion() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        Traslado traslado = recoleccion();
        traslado.asignarResponsable(operadorLuis);
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));
        conRecoleccion(traslado);

        DetalleRentaResponse detalle = servicio.obtener(20L);

        assertEquals("R-2091", detalle.folio());
        assertEquals("Industrias Cajeme", detalle.cliente());
        assertEquals(EstadoRenta.EN_CURSO, detalle.estado());
        // El equipo cancelado no se muestra
        assertEquals(List.of("MC-030"),
                detalle.equipos().stream().map(DetalleRentaResponse.EquipoDeRenta::numeroUnidad).toList());
        assertEquals("Toyota 8FGU25", detalle.equipos().get(0).equipo());
        assertEquals("TR-0091", detalle.trasladoRecoleccion().folio());
        assertEquals("Luis Valenzuela", detalle.trasladoRecoleccion().responsable());
    }

    @Test
    void obtener_sinRecoleccion_laDejaVacia() {
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));
        conRecoleccion(null);

        assertNull(servicio.obtener(20L).trasladoRecoleccion());
    }

    @Test
    void obtener_responde404_cuandoNoExiste() {
        when(rentaRepositorio.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> servicio.obtener(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void confirmarRecoleccion_laPrimeraVez_creaElTraslado() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));
        conRecoleccion(null);

        servicio.confirmarRecoleccion(20L, new HorarioRecoleccionRequest(DESDE, HASTA));

        verify(avanceRenta).programarRecoleccion(renta, DESDE, HASTA);
    }

    @Test
    void confirmarRecoleccion_cuandoYaExiste_cambiaElHorarioDeLaRentaYDelTraslado() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        renta.confirmarRecoleccion(DESDE, HASTA);
        Traslado traslado = recoleccion();
        traslado.asignarResponsable(operadorLuis);
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));
        conRecoleccion(traslado);
        LocalDateTime nuevoDesde = DESDE.plusDays(1);
        LocalDateTime nuevoHasta = HASTA.plusDays(1);
        when(trasladoRepositorio.buscarEmpalmes(100L, nuevoDesde, nuevoHasta, 300L, EstadoTraslado.CANCELADO))
                .thenReturn(List.of());

        DetalleRentaResponse detalle = servicio.confirmarRecoleccion(20L,
                new HorarioRecoleccionRequest(nuevoDesde, nuevoHasta));

        assertEquals(nuevoDesde, renta.getRecoleccionDesde());
        assertEquals(nuevoHasta, renta.getRecoleccionHasta());
        assertEquals(nuevoDesde, traslado.getFechaHoraInicio());
        assertEquals(nuevoHasta, traslado.getFechaHoraFin());
        assertEquals(nuevoDesde, detalle.recoleccionDesde());
        verifyNoInteractions(avanceRenta);
    }

    @Test
    void confirmarRecoleccion_rechazaElCambio_cuandoChocaConOtroTrasladoDelOperador() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        renta.confirmarRecoleccion(DESDE, HASTA);
        Traslado traslado = recoleccion();
        traslado.asignarResponsable(operadorLuis);
        Traslado otro = new Traslado("TR-0095", renta, TipoTraslado.ENTREGA,
                DESDE.plusDays(1), HASTA.plusDays(1));
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));
        conRecoleccion(traslado);
        when(trasladoRepositorio.buscarEmpalmes(any(), any(), any(), any(), any())).thenReturn(List.of(otro));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarRecoleccion(20L,
                        new HorarioRecoleccionRequest(DESDE.plusDays(1), HASTA.plusDays(1))));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("TR-0095"));
        assertEquals(DESDE, traslado.getFechaHoraInicio());
        assertEquals(DESDE, renta.getRecoleccionDesde());
    }

    @Test
    void confirmarRecoleccion_rechazaElCambio_cuandoYaInicio() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        renta.confirmarRecoleccion(DESDE, HASTA);
        Traslado traslado = recoleccion();
        traslado.iniciar();
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));
        conRecoleccion(traslado);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarRecoleccion(20L,
                        new HorarioRecoleccionRequest(DESDE.plusDays(1), HASTA.plusDays(1))));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(DESDE, traslado.getFechaHoraInicio());
    }

    @Test
    void confirmarRecoleccion_rechaza_cuandoLaRentaNoEstaEnCurso() {
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarRecoleccion(20L, new HorarioRecoleccionRequest(DESDE, HASTA)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(trasladoRepositorio, avanceRenta);
    }

    @Test
    void confirmarRecoleccion_rechaza_cuandoElHorarioTerminaAntesDeEmpezar() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarRecoleccion(20L, new HorarioRecoleccionRequest(HASTA, DESDE)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(trasladoRepositorio, avanceRenta);
    }

    @Test
    void confirmarRecoleccion_rechaza_cuandoLaFechaYaPaso() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        when(rentaRepositorio.findById(20L)).thenReturn(Optional.of(renta));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.confirmarRecoleccion(20L, new HorarioRecoleccionRequest(
                        LocalDateTime.of(2020, 1, 1, 9, 0), LocalDateTime.of(2020, 1, 1, 11, 0))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(trasladoRepositorio, avanceRenta);
    }
}