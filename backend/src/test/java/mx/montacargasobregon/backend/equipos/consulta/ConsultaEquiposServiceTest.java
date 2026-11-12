package mx.montacargasobregon.backend.equipos.consulta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.repositorio.EquipoRepository;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

@ExtendWith(MockitoExtension.class)
class ConsultaEquiposServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 10);
    private static final LocalDate FIN = LocalDate.of(2026, 11, 20);

    @Mock
    private EquipoRepository equipoRepositorio;

    @Mock
    private ConsultaEquiposRepository repositorio;

    private ConsultaEquiposService servicio;

    private Cliente cliente;
    private Modelo modelo;

    @BeforeEach
    void setUp() {
        servicio = new ConsultaEquiposService(equipoRepositorio, repositorio);

        cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        modelo = new Modelo("8FGU25", "Toyota", "TOY-8FGU25", null, new BigDecimal("1500"),
                new Categoria("Montacargas"));
    }

    private Equipo equipo(Long id, String numeroUnidad) {
        Equipo e = new Equipo(numeroUnidad, "SN-" + numeroUnidad, 2021, modelo);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private Renta renta(Long id, String folio, LocalDate inicio) {
        Renta r = RentaDePrueba.nueva(folio, cliente, inicio, inicio.plusDays(10), "Calle Sufragio Efectivo 300",
                "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    private EquipoRenta enRenta(Renta renta, Equipo equipo, FaseEquipoRenta fase) {
        EquipoRenta er = renta.agregarEquipo(equipo);
        er.cambiarFase(fase);
        return er;
    }

    @Test
    void consultar_sinFechas_juntaElEstadoDelEquipoConSuRenta() {
        Equipo disponible = equipo(30L, "MC-030");
        Equipo conCliente = equipo(31L, "MC-031");
        Equipo reservado = equipo(32L, "MC-032");
        Equipo reparandose = equipo(33L, "MC-033");
        reparandose.cambiarEstado(EstadoEquipo.EN_MANTENIMIENTO);
        Equipo descompuesto = equipo(34L, "MC-034");
        descompuesto.cambiarEstado(EstadoEquipo.FUERA_SERVICIO);
        Equipo deBaja = equipo(35L, "MC-035");
        deBaja.darDeBaja();

        Renta r2091 = renta(20L, "R-2091", INICIO);
        Renta r2092 = renta(21L, "R-2092", INICIO);
        List<EquipoRenta> activos = List.of(
                enRenta(r2091, conCliente, FaseEquipoRenta.EN_OPERACION),
                enRenta(r2092, reservado, FaseEquipoRenta.RESERVADO),
                enRenta(r2091, reparandose, FaseEquipoRenta.EN_OPERACION));
        when(repositorio.enRentasVigentes(any(), any())).thenReturn(activos);
        when(equipoRepositorio.findAll())
                .thenReturn(List.of(descompuesto, reservado, deBaja, disponible, reparandose, conCliente));

        List<EquipoEstadoResponse> equipos = servicio.consultar(null, null);

        assertEquals(List.of("MC-030", "MC-031", "MC-032", "MC-033", "MC-034"),
                equipos.stream().map(EquipoEstadoResponse::numeroUnidad).toList());
        assertEquals(List.of(
                        EstadoConsultaEquipo.DISPONIBLE,
                        EstadoConsultaEquipo.EN_RENTA,
                        EstadoConsultaEquipo.EN_PREPARACION,
                        EstadoConsultaEquipo.EN_MANTENIMIENTO,
                        EstadoConsultaEquipo.FUERA_SERVICIO),
                equipos.stream().map(EquipoEstadoResponse::estado).toList());

        assertNull(equipos.get(0).rentaId());
        assertEquals("R-2091", equipos.get(1).rentaFolio());
        assertEquals(21L, equipos.get(2).rentaId());
        // En mantenimiento, pero se sigue viendo de qué renta es
        assertEquals("R-2091", equipos.get(3).rentaFolio());
        assertEquals("Toyota 8FGU25", equipos.get(0).equipo());
        assertEquals("Montacargas", equipos.get(0).categoria());
        verify(repositorio, never()).enRentasDelPeriodo(any(), any(), any(), any(), any());
    }

    @Test
    void consultar_conFechas_usaLasRentasDelPeriodo() {
        Equipo equipo = equipo(30L, "MC-030");
        Renta r2091 = renta(20L, "R-2091", INICIO);
        EquipoRenta enRevision = enRenta(r2091, equipo, FaseEquipoRenta.EN_REVISION_ENTRADA);
        when(repositorio.enRentasDelPeriodo(FaseEquipoRenta.EN_REVISION_ENTRADA,
                List.of(FaseEquipoRenta.CANCELADO, FaseEquipoRenta.COMPLETADO),
                List.of(EstadoRenta.PROGRAMADA, EstadoRenta.EN_CURSO),
                INICIO, FIN))
                .thenReturn(List.of(enRevision));
        when(equipoRepositorio.findAll()).thenReturn(List.of(equipo));

        List<EquipoEstadoResponse> equipos = servicio.consultar(INICIO, FIN);

        assertEquals(EstadoConsultaEquipo.EN_PREPARACION, equipos.get(0).estado());
        verify(repositorio, never()).enRentasVigentes(any(), any());
    }

    @Test
    void consultar_prefiereLaRentaEnLaQueEstaConElCliente() {
        Equipo equipo = equipo(30L, "MC-030");
        Renta siguiente = renta(21L, "R-2092", INICIO.plusDays(15));
        Renta actual = renta(20L, "R-2091", INICIO);
        List<EquipoRenta> activos = List.of(
                enRenta(siguiente, equipo, FaseEquipoRenta.RESERVADO),
                enRenta(actual, equipo, FaseEquipoRenta.EN_OPERACION));
        when(repositorio.enRentasVigentes(any(), any())).thenReturn(activos);
        when(equipoRepositorio.findAll()).thenReturn(List.of(equipo));

        EquipoEstadoResponse resultado = servicio.consultar(null, null).get(0);

        assertEquals(EstadoConsultaEquipo.EN_RENTA, resultado.estado());
        assertEquals("R-2091", resultado.rentaFolio());
    }

    @Test
    void consultar_prefiereLaRentaQueEmpiezaAntes_cuandoNingunaTieneAlCliente() {
        Equipo equipo = equipo(30L, "MC-030");
        Renta despues = renta(21L, "R-2092", INICIO.plusDays(15));
        Renta antes = renta(20L, "R-2091", INICIO);
        List<EquipoRenta> activos = List.of(
                enRenta(despues, equipo, FaseEquipoRenta.RESERVADO),
                enRenta(antes, equipo, FaseEquipoRenta.LISTO_ENTREGA));
        when(repositorio.enRentasVigentes(any(), any())).thenReturn(activos);
        when(equipoRepositorio.findAll()).thenReturn(List.of(equipo));

        EquipoEstadoResponse resultado = servicio.consultar(null, null).get(0);

        assertEquals(EstadoConsultaEquipo.EN_PREPARACION, resultado.estado());
        assertEquals("R-2091", resultado.rentaFolio());
    }

    @Test
    void consultar_rechaza_cuandoSoloViene_unaFecha() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.consultar(INICIO, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repositorio, equipoRepositorio);
    }

    @Test
    void consultar_rechaza_cuandoElFinEsAntesDelInicio() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.consultar(FIN, INICIO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repositorio, equipoRepositorio);
    }
}