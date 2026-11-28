package mx.montacargasobregon.backend.cotizaciones.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
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
import mx.montacargasobregon.backend.clientes.servicio.ClienteService;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;
import mx.montacargasobregon.backend.comun.dto.DatosDireccionRequest;
import mx.montacargasobregon.backend.cotizaciones.dto.CorregirCotizacionRequest;
import mx.montacargasobregon.backend.cotizaciones.dto.EventoHistorialResponse;
import mx.montacargasobregon.backend.cotizaciones.modelo.CorreccionCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Descuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.EstadoCotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.ResultadoAutorizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.TipoDescuento;
import mx.montacargasobregon.backend.cotizaciones.repositorio.CotizacionRepository;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.equipos.servicio.EquipoService;
import mx.montacargasobregon.backend.usuarios.TipoUsuario;
import mx.montacargasobregon.backend.usuarios.Usuario;
import mx.montacargasobregon.backend.usuarios.repositorio.UsuarioRepository;

/** Corregir una cotización que gerencia rechazó (HU-32). */
@ExtendWith(MockitoExtension.class)
class CorreccionCotizacionServiceTest {

    private static final String CORREO_AUXILIAR = "daniela.felix@montacargasobregon.mx";

    // Fechas a partir de hoy: la corrección revisa, como al crearla, que no sean de días que ya pasaron
    private static final LocalDate DENTRO_DE_5_DIAS = LocalDate.now(Cotizacion.ZONA_HORARIA).plusDays(5);
    private static final LocalDateTime ENTREGA = DENTRO_DE_5_DIAS.atTime(9, 0);
    private static final LocalDateTime RECOLECCION = DENTRO_DE_5_DIAS.plusDays(2).atTime(17, 0); // 3 días

    @Mock
    private CotizacionService cotizaciones;
    @Mock
    private CotizacionRepository repositorio;
    @Mock
    private ClienteService clienteService;
    @Mock
    private EquipoService equipoService;
    @Mock
    private UsuarioRepository usuarios;

    private CorreccionCotizacionService servicio;
    private Cotizacion cotizacion;
    private Usuario auxiliar;
    private Usuario gerente;
    private DatosDireccion direccionOriginal;

    @BeforeEach
    void preparar() {
        servicio = new CorreccionCotizacionService(cotizaciones,
                new CrearCotizacionService(repositorio, clienteService, equipoService), usuarios);

        auxiliar = new Usuario("AUX-01", "Daniela", "Félix", "Moreno", CORREO_AUXILIAR, "hash", TipoUsuario.AUXILIAR);
        gerente = new Usuario("GER-01", "Patricia", "Romero", "Soto", "patricia.romero@montacargasobregon.mx",
                "hash", TipoUsuario.GERENTE);

        // COT-0156 de la Figma: MC-07 a $1,200 por 5 días, 10 % de descuento, rechazada por gerencia
        Cliente cliente = new Cliente("Laura", "Méndez", "Ríos", "Refaccionaria del Norte", "6871458820",
                "laura@refaccionaria.mx", LocalDate.of(2026, 10, 1));
        direccionOriginal = new DatosDireccion("Carretera Guasave–Los Mochis", "km 4.5", "Parque Industrial",
                "Guasave", "Sinaloa", "81100");
        cotizacion = new Cotizacion("COT-0156", cliente, null,
                LocalDateTime.of(2026, 10, 12, 9, 0), LocalDateTime.of(2026, 10, 16, 17, 0),
                direccionOriginal, new DatosContactoSitio("Laura Méndez Ríos", "6871458820"),
                new BigDecimal("6000.00"), new BigDecimal("960.00"), new BigDecimal("6960.00"));
        ReflectionTestUtils.setField(cotizacion, "id", 156L);
        cotizacion.agregarEquipo(new EquipoCotizacion(equipo(7L, "MC-07", "1200.00"),
                new BigDecimal("1200.00"), new BigDecimal("6000.00")));
        cotizacion.registrarTraslado(new BigDecimal("500.00"), new BigDecimal("500.00")); // Subtotal $7,000
        cotizacion.definirPrecio();
        cotizacion.aplicarDescuento(new Descuento(TipoDescuento.PORCENTAJE, new BigDecimal("10"), "Cliente frecuente"));
        cotizacion.enviarAAutorizacion(auxiliar);
        cotizacion.rechazarDescuento(gerente, "El 10% excede el máximo autorizado (8%)");

        lenient().when(cotizaciones.obtener(156L)).thenReturn(cotizacion);
        lenient().when(usuarios.findByCorreoIgnoreCase(CORREO_AUXILIAR)).thenReturn(Optional.of(auxiliar));
        lenient().when(equipoService.obtener(7L)).thenReturn(equipo(7L, "MC-07", "1200.00"));
        lenient().when(equipoService.obtener(5L)).thenReturn(equipo(5L, "PT-05", "250.00"));
    }

    @Test
    void conDescuentoVuelveAAutorizacionConLosDatosNuevos() {
        Cotizacion c = servicio.corregir(156L, solicitud(List.of(7L, 5L), TipoDescuento.PORCENTAJE, "8"),
                CORREO_AUXILIAR);

        assertEquals(EstadoCotizacion.EN_AUTORIZACION, c.getEstado());
        assertEquals(ResultadoAutorizacion.PENDIENTE, c.getAutorizacion().getResultado());
        assertSame(auxiliar, c.getAutorizacion().getSolicitadaPor());
        assertEquals(new BigDecimal("8"), c.getDescuento().getValor());

        // 3 días: MC-07 $3,600 + PT-05 $750 + traslados $1,000 = $5,350; −8 % = $4,922; IVA $787.52
        assertEquals(3L, c.getDias());
        assertEquals(2, c.getEquipos().size());
        assertEquals(new BigDecimal("5350.00"), c.getSubtotal());
        assertEquals(new BigDecimal("428.00"), c.getMontoDescuento());
        assertEquals(new BigDecimal("787.52"), c.getIva());
        assertEquals(new BigDecimal("5709.52"), c.getTotal());

        // Se actualiza la misma dirección; el folio no cambia
        assertSame(direccionOriginal, c.getDireccionObra());
        assertEquals("Calle 200", c.getDireccionObra().getCalle());
        assertEquals("Ing. Raúl Soto", c.getContactoObra().getNombre());
        assertEquals("COT-0156", c.getFolio());
    }

    @Test
    void sinDescuentoQuedaEnBorrador() {
        Cotizacion c = servicio.corregir(156L, solicitud(List.of(7L), TipoDescuento.NINGUNO, null), CORREO_AUXILIAR);

        assertEquals(EstadoCotizacion.BORRADOR, c.getEstado());
        assertNull(c.getDescuento());
        assertNull(c.getAutorizacion());
        assertTrue(c.isPrecioDefinido()); // Ya puede sacar el PDF
        assertFalse(c.getCorrecciones().get(0).isReenviada());
    }

    @Test
    void guardaElRechazoEnElHistorial() {
        Cotizacion c = servicio.corregir(156L, solicitud(List.of(7L), TipoDescuento.PORCENTAJE, "8"),
                CORREO_AUXILIAR);

        CorreccionCotizacion correccion = c.getCorrecciones().get(0);
        assertSame(gerente, correccion.getRechazadaPor());
        assertEquals("El 10% excede el máximo autorizado (8%)", correccion.getComentarioRechazo());
        assertSame(auxiliar, correccion.getCorregidaPor());
        assertTrue(correccion.isReenviada());
    }

    @Test
    void elHistorialJuntaCadaRechazoConSuCorreccion() {
        servicio.corregir(156L, solicitud(List.of(7L), TipoDescuento.PORCENTAJE, "8"), CORREO_AUXILIAR);
        cotizacion.rechazarDescuento(gerente, "Con 8% tampoco sale");

        List<EventoHistorialResponse> historial = EventoHistorialResponse.historialDe(cotizacion);

        assertEquals(List.of("RECHAZADA", "REENVIADA", "RECHAZADA"),
                historial.stream().map(EventoHistorialResponse::tipo).toList());
        assertEquals("El 10% excede el máximo autorizado (8%)", historial.get(0).comentario());
        assertEquals("Daniela Félix", historial.get(1).usuario());
        assertEquals("Con 8% tampoco sale", historial.get(2).comentario());
    }

    @Test
    void soloSeCorrigeUnaRechazada() {
        Cotizacion borrador = new Cotizacion("COT-0157", cotizacion.getCliente(), null, ENTREGA, RECOLECCION,
                new DatosDireccion("Calle 200", "1500", "Centro", "Cd. Obregón", "Sonora", "85000"),
                new DatosContactoSitio("Ing. Raúl Soto", "6442104455"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(cotizaciones.obtener(157L)).thenReturn(borrador);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.corregir(157L, solicitud(List.of(7L), TipoDescuento.NINGUNO, null), CORREO_AUXILIAR));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals(EstadoCotizacion.BORRADOR, borrador.getEstado());
    }

    @Test
    void unMontoMayorAlSubtotalNuevoNoSeGuarda() {
        // Con 3 días el subtotal nuevo es $4,600: un descuento de $5,000 ya no cabe
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.corregir(156L, solicitud(List.of(7L), TipoDescuento.MONTO, "5000"), CORREO_AUXILIAR));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(EstadoCotizacion.RECHAZADA, cotizacion.getEstado());
        assertTrue(cotizacion.getCorrecciones().isEmpty());
    }

    @Test
    void conDescuentoPideElMotivo() {
        CorregirCotizacionRequest sinMotivo = new CorregirCotizacionRequest(ENTREGA, RECOLECCION, direccion(),
                "Ing. Raúl Soto", "6442104455", List.of(7L), TipoDescuento.PORCENTAJE, new BigDecimal("8"), "  ");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.corregir(156L, sinMotivo, CORREO_AUXILIAR));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(EstadoCotizacion.RECHAZADA, cotizacion.getEstado());
    }

    @Test
    void noAceptaFechasQueYaPasaron() {
        LocalDateTime ayer = LocalDate.now(Cotizacion.ZONA_HORARIA).minusDays(1).atTime(9, 0);
        CorregirCotizacionRequest conFechaVieja = new CorregirCotizacionRequest(ayer, RECOLECCION, direccion(),
                "Ing. Raúl Soto", "6442104455", List.of(7L), TipoDescuento.NINGUNO, null, null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.corregir(156L, conFechaVieja, CORREO_AUXILIAR));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(EstadoCotizacion.RECHAZADA, cotizacion.getEstado());
    }

    private static CorregirCotizacionRequest solicitud(List<Long> equipoIds, TipoDescuento tipo, String valor) {
        return new CorregirCotizacionRequest(ENTREGA, RECOLECCION, direccion(), " Ing. Raúl Soto ", "6442104455",
                equipoIds, tipo, valor == null ? null : new BigDecimal(valor),
                valor == null ? null : "Cliente frecuente, se ajusta al máximo");
    }

    private static DatosDireccionRequest direccion() {
        return new DatosDireccionRequest("Calle 200", "1500", "Centro", "Cd. Obregón", "Sonora", "85000");
    }

    private static Equipo equipo(Long id, String numeroUnidad, String tarifaDia) {
        Modelo modelo = new Modelo("Modelo " + numeroUnidad, "Toyota", "COD-" + id, null,
                new BigDecimal(tarifaDia), new Categoria("Montacargas"));
        Equipo equipo = new Equipo(numeroUnidad, "SERIE-" + id, 2022, modelo);
        equipo.cambiarEstado(EstadoEquipo.OPERATIVO);
        ReflectionTestUtils.setField(equipo, "id", id);
        return equipo;
    }
}
