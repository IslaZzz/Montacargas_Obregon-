package mx.montacargasobregon.backend.cotizaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import mx.montacargasobregon.backend.clientes.modelo.DireccionFiscal;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.clientes.servicio.ClienteService;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.comun.dto.DatosDireccionRequest;
import mx.montacargasobregon.backend.cotizaciones.dto.CrearCotizacionRequest;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.repositorio.CotizacionRepository;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.servicio.EquipoService;

@ExtendWith(MockitoExtension.class)
class CrearCotizacionServiceTest {

    private static final LocalDateTime ENTREGA = LocalDateTime.of(2026, 10, 12, 9, 0);
    private static final LocalDateTime RECOLECCION = LocalDateTime.of(2026, 10, 16, 17, 0);

    @Mock
    private CotizacionRepository repositorio;
    @Mock
    private ClienteService clienteService;
    @Mock
    private EquipoService equipoService;

    private CrearCotizacionService servicio;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        servicio = new CrearCotizacionService(repositorio, clienteService, equipoService);

        cliente = new Cliente("Ramón", "Gutiérrez", "Soto", "Constructora del Valle de Guasave",
                "6871234567", "compras@cdvguasave.mx", LocalDate.of(2026, 10, 1));
        ReflectionTestUtils.setField(cliente, "id", 1L);
        RazonSocial razon = new RazonSocial("Constructora del Valle de Guasave S.A. de C.V.", "CVG150312KD4",
                new DireccionFiscal("Blvd. Central", "1250", "Centro", "Guasave", "81000"));
        ReflectionTestUtils.setField(razon, "id", 10L);
        cliente.agregarRazonSocial(razon);
    }

    @Test
    void creaBorradorConFolioYTotalesDelFigma() {
        when(clienteService.obtener(1L)).thenReturn(cliente);
        when(equipoService.obtener(7L)).thenReturn(equipo(7L, "MC-07", "1200.00"));
        when(equipoService.obtener(5L)).thenReturn(equipo(5L, "PT-05", "250.00"));
        when(repositorio.ultimoId()).thenReturn(0L);
        when(repositorio.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Cotizacion c = servicio.crear(solicitud(10L, ENTREGA, RECOLECCION, List.of(7L, 5L)));

        assertEquals("COT-0001", c.getFolio());
        assertEquals(EstadoCotizacion.BORRADOR, c.getEstado());
        assertEquals(5L, c.getDias());
        assertEquals(new BigDecimal("1200.00"), c.getEquipos().get(0).getTarifa());
        assertEquals(new BigDecimal("6000.00"), c.getEquipos().get(0).getImporte());
        assertEquals(new BigDecimal("1250.00"), c.getEquipos().get(1).getImporte());
        assertEquals(new BigDecimal("7250.00"), c.getSubtotal());
        assertEquals(new BigDecimal("1160.00"), c.getIva());
        assertEquals(new BigDecimal("8410.00"), c.getTotal());
    }

    @Test
    void guardaLaDireccionYElContactoDeLaObraSinEspaciosDeMas() {
        when(clienteService.obtener(1L)).thenReturn(cliente);
        when(equipoService.obtener(7L)).thenReturn(equipo(7L, "MC-07", "1200.00"));
        when(repositorio.ultimoId()).thenReturn(0L);
        when(repositorio.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        var direccion = new DatosDireccionRequest(" Carretera Guasave–Los Mochis ", "km 4.5", "Parque Industrial",
                "Guasave", " Sinaloa ", "81100", "   ");

        Cotizacion c = servicio.crear(solicitud(10L, ENTREGA, RECOLECCION, List.of(7L), direccion));

        DatosDireccion obra = c.getDireccionObra();
        assertEquals("Carretera Guasave–Los Mochis", obra.getCalle());
        assertEquals("Sinaloa", obra.getEstado());
        assertEquals("81100", obra.getCodigoPostal());
        assertNull(obra.getReferencias());
        assertEquals("Ing. Raúl Soto", c.getContactoObra().getNombre());
        assertEquals("6872104455", c.getContactoObra().getTelefono());
        assertEquals(BigDecimal.ZERO, c.getTrasladoEntrega());
        assertNull(c.getDescuento());
    }

    @Test
    void elFolioSigueAlUltimoGuardado() {
        when(clienteService.obtener(1L)).thenReturn(cliente);
        when(equipoService.obtener(7L)).thenReturn(equipo(7L, "MC-07", "1200.00"));
        when(repositorio.ultimoId()).thenReturn(41L);
        when(repositorio.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Cotizacion c = servicio.crear(solicitud(10L, ENTREGA, RECOLECCION, List.of(7L)));

        assertEquals("COT-0042", c.getFolio());
    }

    @Test
    void razonSocialDeOtroClienteFalla() {
        when(clienteService.obtener(1L)).thenReturn(cliente);

        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(99L, ENTREGA, RECOLECCION, List.of(7L))));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void recoleccionAntesDeLaEntregaFalla() {
        when(clienteService.obtener(1L)).thenReturn(cliente);

        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(10L, ENTREGA, ENTREGA.minusHours(1), List.of(7L))));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void equipoRepetidoFalla() {
        when(clienteService.obtener(1L)).thenReturn(cliente);

        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(10L, ENTREGA, RECOLECCION, List.of(7L, 7L))));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void equipoEnMantenimientoNoSePuedeCotizar() {
        when(clienteService.obtener(1L)).thenReturn(cliente);
        Equipo enTaller = equipo(7L, "MC-07", "1200.00");
        enTaller.cambiarEstado(EstadoEquipo.EN_MANTENIMIENTO);
        when(equipoService.obtener(7L)).thenReturn(enTaller);

        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(solicitud(10L, ENTREGA, RECOLECCION, List.of(7L))));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private static Equipo equipo(Long id, String numeroUnidad, String tarifaDia) {
        Modelo modelo = new Modelo("Modelo " + numeroUnidad, "Toyota", "COD-" + id, null,
                new BigDecimal(tarifaDia), new Categoria("Montacargas"));
        Equipo equipo = new Equipo(numeroUnidad, "SERIE-" + id, 2022, modelo);
        ReflectionTestUtils.setField(equipo, "id", id);
        return equipo;
    }

    private static CrearCotizacionRequest solicitud(Long razonSocialId, LocalDateTime entrega,
                                                    LocalDateTime recoleccion, List<Long> equipoIds) {
        return solicitud(razonSocialId, entrega, recoleccion, equipoIds,
                new DatosDireccionRequest("Carretera Guasave–Los Mochis", "km 4.5", "Parque Industrial", "Guasave",
                        "Sinaloa", "81100", "Portón azul. Preguntar por el Ing. Soto."));
    }

    private static CrearCotizacionRequest solicitud(Long razonSocialId, LocalDateTime entrega,
                                                    LocalDateTime recoleccion, List<Long> equipoIds,
                                                    DatosDireccionRequest direccionObra) {
        return new CrearCotizacionRequest(
                1L, razonSocialId, entrega, recoleccion, direccionObra,
                "Ing. Raúl Soto", "6872104455", equipoIds);
    }
}
