package mx.montacargasobregon.backend.movil.traslados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.avisos.servicio.AvisoOperacionService;
import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.TipoFirma;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.cartas.servicio.CartaResponsivaService;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.movil.sesion.PersonalActual;
import mx.montacargasobregon.backend.operacion.servicio.AvanceRentaService;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.rentas.RentaDePrueba;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
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
    private CartaResponsivaService cartaServicio;

    @Mock
    private AvanceRentaService avanceRenta;

    @Mock
    private AvisoOperacionService avisos;

    @Mock
    private EvidenciaRecoleccionService evidencia;

    private TareasTrasladoService servicio;

    private final Personal operador = personal(7L, "OP-01");
    private final PersonalActual actual = PersonalActual.de(operador);

    private Renta renta;
    private EquipoRenta mc030;
    private EquipoRenta mc031;
    private Traslado entrega;

    @BeforeEach
    void setUp() {
        servicio = new TareasTrasladoService(trasladoRepositorio, cartaRepositorio, cartaServicio, avanceRenta,
                avisos, evidencia);

        Cliente cliente = new Cliente("Ramón", "Gutiérrez", null, "Industrias Cajeme",
                "6441000001", "compras@industriascajeme.mx", LocalDate.of(2026, 1, 1));
        renta = RentaDePrueba.nueva("R-2091", cliente, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 26),
                "Calle Sufragio Efectivo 300", "Ramón Gutiérrez", "6441000001");
        ReflectionTestUtils.setField(renta, "id", 90L);
        mc030 = renta.agregarEquipo(new Equipo("MC-030", "SN-030", 2021, null));
        mc031 = renta.agregarEquipo(new Equipo("MC-031", "SN-031", 2021, null));
        ReflectionTestUtils.setField(mc030, "id", 1L);
        ReflectionTestUtils.setField(mc031, "id", 2L);

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
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.of(cartaFirmada()));

        DetalleTareaResponse detalle = servicio.iniciar(50L, actual);

        assertEquals(EstadoTraslado.EN_CURSO, entrega.getEstado());
        assertTrue(detalle.puedeCompletar());
        assertFalse(detalle.puedeIniciar());
        verify(avanceRenta).trasladoIniciado(entrega);
        verify(avisos).trasladoIniciado(entrega);
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
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.of(cartaFirmada()));

        servicio.completar(50L, actual);

        assertEquals(EstadoTraslado.COMPLETADO, entrega.getEstado());
        verify(avanceRenta).trasladoRealizado(entrega);
        verify(avisos).entregaRegistrada(entrega);
    }

    // ---- HU 11: la entrega pide la carta firmada ----

    @Test
    void noSeRegistraLaEntregaConLaCartaSinFirmar() {
        entrega.iniciar();
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.of(cartaGenerada()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.completar(50L, actual));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(TareasTrasladoService.SIN_CARTA_FIRMADA, error.getReason());
        assertEquals(EstadoTraslado.EN_CURSO, entrega.getEstado());
        verify(avanceRenta, never()).trasladoRealizado(any());
        verifyNoInteractions(avisos);
    }

    @Test
    void noSeRegistraLaEntregaSiLaRentaNoTieneCarta() {
        entrega.iniciar();
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.completar(50L, actual));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(avanceRenta, never()).trasladoRealizado(any());
    }

    @Test
    void elDetalleDiceQueLaCartaEstaPendienteYNoDejaCompletar() {
        entrega.iniciar();
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.of(cartaGenerada()));

        DetalleTareaResponse detalle = servicio.detalle(50L, actual);

        assertEquals(CartaTarea.PENDIENTE, detalle.carta());
        assertFalse(detalle.puedeCompletar());
    }

    @Test
    void conLaCartaFirmadaElDetalleDejaCompletar() {
        entrega.iniciar();
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));
        when(cartaRepositorio.findByRentaId(90L)).thenReturn(Optional.of(cartaFirmada()));

        DetalleTareaResponse detalle = servicio.detalle(50L, actual);

        assertEquals(CartaTarea.FIRMADA, detalle.carta());
        assertTrue(detalle.puedeCompletar());
    }

    @Test
    void laRecoleccionNoPideCarta() {
        Traslado recoleccion = traslado(51L, TipoTraslado.RECOLECCION);
        recoleccion.asignarResponsable(operador);
        recoleccion.iniciar();
        when(trasladoRepositorio.findById(51L)).thenReturn(Optional.of(recoleccion));

        DetalleTareaResponse detalle = servicio.completar(51L, actual);

        assertNull(detalle.carta());
        assertEquals(EstadoTraslado.COMPLETADO, recoleccion.getEstado());
        verify(avanceRenta).trasladoRealizado(recoleccion);
        verify(avisos).equiposRecibidos(recoleccion);
    }

    @Test
    void adjuntarLaCartaRegistraLaFirmaFisicaConElOperador() throws Exception {
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));
        MockMultipartFile foto = new MockMultipartFile("archivo", "carta.jpg", "image/jpeg",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1 });

        servicio.adjuntarCarta(50L, "Ramón Gutiérrez", foto, actual);

        verify(cartaServicio).registrarFirma(eq(90L), eq(TipoFirma.FISICA), any(), eq("Ramón Gutiérrez"),
                eq(foto.getBytes()), eq("Rafael Amaya"), eq(TareasTrasladoService.ROL_OPERADOR));
    }

    @Test
    void noSeAdjuntaCartaEnUnaRecoleccion() {
        Traslado recoleccion = traslado(51L, TipoTraslado.RECOLECCION);
        recoleccion.asignarResponsable(operador);
        when(trasladoRepositorio.findById(51L)).thenReturn(Optional.of(recoleccion));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.adjuntarCarta(51L, "Ramón Gutiérrez", null, actual));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(cartaServicio);
    }

    // ---- HU 13 y 14: la recolección se confirma con las fotos de evidencia ----

    @Test
    void noSeConfirmaLaRecoleccionSinLasFotos() {
        Traslado recoleccion = recoleccionEnSitio();
        when(trasladoRepositorio.findById(51L)).thenReturn(Optional.of(recoleccion));
        when(evidencia.motivo(recoleccion)).thenReturn("Primero registra las 8 fotografías de evidencia (faltan 3).");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.iniciar(51L, actual));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("faltan 3"));
        assertEquals(EstadoTraslado.PROGRAMADO, recoleccion.getEstado());
        verifyNoInteractions(avanceRenta, avisos);
    }

    @Test
    void conLasFotosCompletasSeConfirmaLaRecoleccion() {
        Traslado recoleccion = recoleccionEnSitio();
        when(trasladoRepositorio.findById(51L)).thenReturn(Optional.of(recoleccion));
        when(evidencia.motivo(recoleccion)).thenReturn(null);

        servicio.iniciar(51L, actual);

        assertEquals(EstadoTraslado.EN_CURSO, recoleccion.getEstado());
        verify(avanceRenta).trasladoIniciado(recoleccion);
        verify(avisos).recoleccionConfirmada(recoleccion);
    }

    @Test
    void seSubeLaFotoDeUnEquipoDeLaRecoleccion() {
        Traslado recoleccion = recoleccionEnSitio();
        when(trasladoRepositorio.findById(51L)).thenReturn(Optional.of(recoleccion));
        MockMultipartFile foto = new MockMultipartFile("archivo", "frente.jpg", "image/jpeg", new byte[] { 1 });

        servicio.subirEvidencia(51L, 1L, "Frente", foto, actual);

        verify(evidencia).subir(recoleccion, mc030, "Frente", foto, actual);
    }

    @Test
    void lasFotosNoSeCambianDespuesDeConfirmar() {
        Traslado recoleccion = recoleccionEnSitio();
        recoleccion.iniciar();
        when(trasladoRepositorio.findById(51L)).thenReturn(Optional.of(recoleccion));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.subirEvidencia(51L, 1L, "Frente", null, actual));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(evidencia);
    }

    @Test
    void noSeSubeFotoDeUnEquipoAjenoNiDeOtraVista() {
        Traslado recoleccion = recoleccionEnSitio();
        when(trasladoRepositorio.findById(51L)).thenReturn(Optional.of(recoleccion));

        ResponseStatusException ajeno = assertThrows(ResponseStatusException.class,
                () -> servicio.subirEvidencia(51L, 99L, "Frente", null, actual));
        ResponseStatusException vista = assertThrows(ResponseStatusException.class,
                () -> servicio.subirEvidencia(51L, 1L, "Techo", null, actual));

        assertEquals(HttpStatus.BAD_REQUEST, ajeno.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, vista.getStatusCode());
        verifyNoInteractions(evidencia);
    }

    @Test
    void laRecoleccionRecibidaHoySeQuedaEnLaLista() {
        Traslado recibida = recoleccionEnSitio();
        recibida.iniciar();
        recibida.completar();
        when(trasladoRepositorio.buscarPorOperador(7L)).thenReturn(List.of(recibida));
        when(cartaRepositorio.findByRentaIdIn(Set.of(90L))).thenReturn(List.of());

        List<TareaResponse> tareas = servicio.misTareas(actual, TipoTraslado.RECOLECCION);

        assertEquals(1, tareas.size());
        assertEquals(EstadoTraslado.COMPLETADO, tareas.get(0).estado());
    }

    @Test
    void noSePuedeAbrirUnaTareaDeOtroOperador() {
        PersonalActual otro = PersonalActual.de(personal(8L, "OP-02"));
        when(trasladoRepositorio.findById(50L)).thenReturn(Optional.of(entrega));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.detalle(50L, otro));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    /** Una recolección programada, con la renta en curso y los equipos con el cliente. */
    private Traslado recoleccionEnSitio() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);
        mc030.cambiarFase(FaseEquipoRenta.EN_OPERACION);
        mc031.cambiarFase(FaseEquipoRenta.EN_OPERACION);
        Traslado recoleccion = traslado(51L, TipoTraslado.RECOLECCION);
        recoleccion.asignarResponsable(operador);
        return recoleccion;
    }

    private CartaResponsiva cartaGenerada() {
        return new CartaResponsiva(renta, "", new byte[] { '%', 'P', 'D', 'F' });
    }

    private CartaResponsiva cartaFirmada() {
        CartaResponsiva carta = cartaGenerada();
        carta.registrarFirma(TipoFirma.FISICA, DESDE, "Ramón Gutiérrez", "Rafael Amaya", "Operador de traslado",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }, "image/jpeg");
        return carta;
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