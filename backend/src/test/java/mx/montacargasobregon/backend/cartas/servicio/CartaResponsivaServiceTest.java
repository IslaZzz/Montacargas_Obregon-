package mx.montacargasobregon.backend.cartas.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import mx.montacargasobregon.backend.cartas.dto.CambioCarta;
import mx.montacargasobregon.backend.cartas.dto.CartaResponse;
import mx.montacargasobregon.backend.cartas.modelo.CartaResponsiva;
import mx.montacargasobregon.backend.cartas.modelo.EstadoCarta;
import mx.montacargasobregon.backend.cartas.modelo.TipoFirma;
import mx.montacargasobregon.backend.cartas.repositorio.CartaResponsivaRepository;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.modelo.DireccionFiscal;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.equipos.modelo.Categoria;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.rentas.repositorio.RentaRepository;

@ExtendWith(MockitoExtension.class)
class CartaResponsivaServiceTest {

    private static final Long RENTA_ID = 7L;
    private static final LocalDate INICIO = LocalDate.of(2026, 10, 12);
    private static final LocalDateTime AYER = LocalDateTime.now(CartaResponsiva.ZONA_HORARIA).minusDays(1);
    private static final byte[] PDF = { '%', 'P', 'D', 'F', '-', '1' };
    private static final byte[] JPG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0 };
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A };

    @Mock
    private RentaRepository rentas;
    @Mock
    private CartaResponsivaRepository cartas;
    @Mock
    private CorreoCartaService correo;

    private CartaResponsivaService servicio;
    private Renta renta;

    @BeforeEach
    void preparar() {
        // El PDF se arma de verdad: así también se prueba que no truena con datos reales
        servicio = new CartaResponsivaService(rentas, cartas, new CartaPdfService(), correo);

        Cliente cliente = new Cliente("Juan", "Pérez", null, "Constructora Valle Yaqui",
                "6440000000", "juan.perez@ejemplo.com", LocalDate.of(2026, 9, 1));
        cliente.agregarRazonSocial(new RazonSocial("Constructora Valle Yaqui S.A. de C.V.", "CVY010203AB1",
                new DireccionFiscal("Calle Ejemplo", "123", "Centro", "Cd. Obregón", "85000")));

        renta = new Renta("R-1042", cliente, INICIO, INICIO.plusDays(30),
                "Calle Ejemplo 123, Col. Centro, 85000", "Juan Pérez", "6440000000");
        ReflectionTestUtils.setField(renta, "id", RENTA_ID);
        renta.confirmarEntrega(INICIO.atTime(8, 0), INICIO.atTime(9, 30));
        renta.agregarEquipo(equipo("MC-09", "AT0000000"));

        // lenient: las pruebas que solo arman el PDF no buscan la renta
        lenient().when(rentas.findById(RENTA_ID)).thenReturn(Optional.of(renta));
    }

    @Test
    void sinCartaEstaSinGenerarYSePuedeGenerar() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.empty());

        CartaResponse carta = servicio.consultar(RENTA_ID);

        assertEquals(EstadoCarta.SIN_GENERAR, carta.estado());
        assertTrue(carta.puedeGenerar());
        assertFalse(carta.tieneArchivo());
        assertNull(carta.version());
        assertEquals("juan.perez@ejemplo.com", carta.correoCliente());
    }

    @Test
    void generaLaVersionUnoConSuPdf() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.empty());
        when(cartas.save(any())).thenAnswer(i -> i.getArgument(0));

        CartaResponse carta = servicio.generar(RENTA_ID);

        assertEquals(EstadoCarta.GENERADA, carta.estado());
        assertEquals(1, carta.version());
        assertEquals("Carta_responsiva_R-1042_v1.pdf", carta.nombreArchivo());
        assertTrue(carta.tieneArchivo());
        assertTrue(carta.cambios().isEmpty());
    }

    @Test
    void elPdfEsUnArchivoPdf() {
        byte[] pdf = new CartaPdfService().generar(DatosCarta.de(renta));

        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    @Test
    void noGeneraSiFaltanDatosYDiceCualesEnOrden() {
        ReflectionTestUtils.setField(renta, "entregaDesde", null);
        ReflectionTestUtils.setField(renta.getCliente(), "correo", " ");
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.empty());

        DatosFaltantesException error = assertThrows(DatosFaltantesException.class,
                () -> servicio.generar(RENTA_ID));

        assertEquals(List.of("Correo electrónico del cliente", "Fecha de entrega"),
                List.copyOf(error.getFaltantes().keySet()));
        assertEquals("Cliente", error.getFaltantes().get("Correo electrónico del cliente"));
        assertEquals("Renta", error.getFaltantes().get("Fecha de entrega"));
        verify(cartas, never()).save(any());
    }

    @Test
    void sinRazonSocialSiSeGenera() {
        ReflectionTestUtils.setField(renta.getCliente(), "razonesSociales", new java.util.ArrayList<>());

        assertTrue(DatosCarta.de(renta).faltantes().isEmpty());
        assertEquals("%PDF", new String(new CartaPdfService().generar(DatosCarta.de(renta)), 0, 4));
    }

    @Test
    void sinEquiposActivosFaltanLosEquipos() {
        renta.getEquipos().forEach(e -> e.cambiarFase(FaseEquipoRenta.CANCELADO));
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.empty());

        DatosFaltantesException error = assertThrows(DatosFaltantesException.class,
                () -> servicio.generar(RENTA_ID));

        assertEquals(List.of("Equipos de la renta"), List.copyOf(error.getFaltantes().keySet()));
    }

    @Test
    void soloSeGeneraParaRentasProgramadas() {
        renta.cambiarEstado(EstadoRenta.EN_CURSO);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.generar(RENTA_ID));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(cartas, never()).save(any());
    }

    @Test
    void alRegenerarSubeLaVersionYVuelveAGenerada() {
        CartaResponsiva existente = cartaGenerada();
        existente.cambiarEstado(EstadoCarta.ENVIADA);
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(existente));

        CartaResponse carta = servicio.generar(RENTA_ID);

        assertEquals(2, carta.version());
        assertEquals(EstadoCarta.GENERADA, carta.estado());
        assertEquals("Carta_responsiva_R-1042_v2.pdf", carta.nombreArchivo());
        verify(cartas, never()).save(any()); // Ya estaba guardada: JPA guarda el cambio al terminar
    }

    @Test
    void unaCartaFirmadaNoSeRegenera() {
        CartaResponsiva firmada = cartaGenerada();
        firmada.cambiarEstado(EstadoCarta.FIRMADA);
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(firmada));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.generar(RENTA_ID));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertFalse(servicio.consultar(RENTA_ID).puedeGenerar());
    }

    @Test
    void avisaQueDatosCambiaronDespuesDeGenerarla() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(cartaGenerada()));
        ReflectionTestUtils.setField(renta, "fechaFin", INICIO.plusDays(27));

        List<CambioCarta> cambios = servicio.consultar(RENTA_ID).cambios();

        assertEquals(List.of(
                new CambioCarta("Fecha final", "11/11/2026", "08/11/2026"),
                new CambioCarta("Periodo", "30 días", "27 días")), cambios);
    }

    @Test
    void sinArchivoNoHayPdf() {
        // Como las cartas de los datos de prueba
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(new CartaResponsiva(renta, null, null)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.archivo(RENTA_ID));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void laRentaQueNoExisteResponde404() {
        when(rentas.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.consultar(99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void elResumenSeGuardaYSeLeeIgual() {
        var datos = DatosCarta.de(renta).resumen();

        assertEquals(datos, ResumenCarta.leer(ResumenCarta.texto(datos)));
        assertEquals("12 de octubre de 2026", datos.get("Fecha de entrega"));
        assertEquals("MC-09 Toyota 8FGU25 (serie AT0000000)", datos.get("Equipos"));
    }

    @Test
    void alEnviarlaQuedaEnviadaConFechaYCorreo() {
        CartaResponsiva carta = cartaGenerada();
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(carta));

        CartaResponse enviada = servicio.enviar(RENTA_ID, "Hola Juan, aquí va la carta.");

        verify(correo).enviar(eq("juan.perez@ejemplo.com"), eq("R-1042"), eq("Hola Juan, aquí va la carta."),
                eq("Carta_responsiva_R-1042_v1.pdf"), any());
        assertEquals(EstadoCarta.ENVIADA, enviada.estado());
        assertNotNull(enviada.fechaEnvio());
        assertEquals("juan.perez@ejemplo.com", enviada.correoEnvio());
        assertTrue(enviada.puedeEnviar()); // Se puede reenviar
    }

    @Test
    void sinMensajeSeEnviaElSugerido() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(cartaGenerada()));
        String sugerido = "Hola Juan, te compartimos la carta responsiva de la renta R-1042, "
                + "con el equipo Toyota 8FGU25 (MC-09), para tu firma electrónica.";

        CartaResponse enviada = servicio.enviar(RENTA_ID, "  ");

        verify(correo).enviar(anyString(), anyString(), eq(sugerido), anyString(), any());
        assertEquals(sugerido, enviada.mensajeSugerido());
    }

    @Test
    void siFallaElEnvioSeQuedaGenerada() {
        CartaResponsiva carta = cartaGenerada();
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(carta));
        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No hubo conexión con el servicio de correo."))
                .when(correo).enviar(anyString(), anyString(), anyString(), anyString(), any());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.enviar(RENTA_ID, null));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        assertEquals(EstadoCarta.GENERADA, carta.getEstado());
        assertNull(carta.getFechaEnvio());
    }

    @Test
    void noSeEnviaSinHaberlaGenerado() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.enviar(RENTA_ID, null));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(correo, never()).enviar(anyString(), anyString(), anyString(), anyString(), any());
        assertFalse(servicio.consultar(RENTA_ID).puedeEnviar());
    }

    @Test
    void unaCartaFirmadaNoSeEnvia() {
        CartaResponsiva firmada = cartaGenerada();
        firmada.cambiarEstado(EstadoCarta.FIRMADA);
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(firmada));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.enviar(RENTA_ID, null));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(correo, never()).enviar(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void alRegenerarHayQueVolverAEnviarla() {
        CartaResponsiva carta = cartaGenerada();
        carta.registrarEnvio("juan.perez@ejemplo.com");
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(carta));

        CartaResponse regenerada = servicio.generar(RENTA_ID);

        assertEquals(EstadoCarta.GENERADA, regenerada.estado());
        assertNull(regenerada.fechaEnvio());
        assertNull(regenerada.correoEnvio());
    }

    @Test
    void elMensajeSugeridoNombraTodosLosEquipos() {
        renta.agregarEquipo(equipo("MC-10", "AT0000001"));

        assertEquals("Hola Juan, te compartimos la carta responsiva de la renta R-1042, con los equipos "
                + "Toyota 8FGU25 (MC-09) y Toyota 8FGU25 (MC-10), para tu firma electrónica.",
                CartaResponsivaService.mensajeSugerido(renta, DatosCarta.de(renta)));
    }

    // ===== Firma (OHU-05) =====

    @Test
    void registraLaFirmaFisicaConLaFotoYQuedaFirmada() {
        CartaResponsiva carta = cartaGenerada();
        carta.registrarEnvio("juan.perez@ejemplo.com");
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(carta));

        CartaResponse firmada = servicio.registrarFirma(RENTA_ID, TipoFirma.FISICA, AYER, " Juan Pérez ", JPG);

        assertEquals(EstadoCarta.FIRMADA, firmada.estado());
        assertEquals(TipoFirma.FISICA, firmada.firma().tipo());
        assertEquals(AYER, firmada.firma().fecha());
        assertEquals("Juan Pérez", firmada.firma().firmante());
        // Quién y cuándo se registró
        assertEquals(CartaResponsivaService.USUARIO_PROVISIONAL, firmada.firma().registradaPor());
        assertEquals(CartaResponsivaService.ROL_PROVISIONAL, firmada.firma().rolRegistro());
        assertNotNull(firmada.firma().registradaEn());
        assertEquals("Carta_firmada_R-1042.jpg", firmada.firma().nombreArchivo());
        // Ya no se regenera, no se envía ni se vuelve a firmar
        assertFalse(firmada.puedeGenerar());
        assertFalse(firmada.puedeEnviar());
        assertFalse(firmada.puedeFirmar());
    }

    @Test
    void laFirmaElectronicaGuardaElPdfQueRegresoElCliente() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(cartaGenerada()));

        servicio.registrarFirma(RENTA_ID, TipoFirma.ELECTRONICA, AYER, "Juan Pérez", PDF);
        CartaResponsivaService.ArchivoCarta archivo = servicio.archivoFirmado(RENTA_ID);

        assertEquals("Carta_firmada_R-1042.pdf", archivo.nombre());
        assertEquals("application/pdf", archivo.tipo());
        assertEquals(PDF, archivo.contenido());
    }

    @Test
    void sinEvidenciaNoSeRegistraLaFirmaFisica() {
        CartaResponsiva carta = cartaGenerada();
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(carta));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrarFirma(RENTA_ID, TipoFirma.FISICA, AYER, "Juan Pérez", null));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Adjunta la carta firmada para registrar la firma.", error.getReason());
        assertEquals(EstadoCarta.GENERADA, carta.getEstado());
    }

    @Test
    void laFirmaElectronicaTambienPideElPdf() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(cartaGenerada()));

        ResponseStatusException sinArchivo = assertThrows(ResponseStatusException.class,
                () -> servicio.registrarFirma(RENTA_ID, TipoFirma.ELECTRONICA, AYER, "Juan Pérez", new byte[0]));
        ResponseStatusException foto = assertThrows(ResponseStatusException.class,
                () -> servicio.registrarFirma(RENTA_ID, TipoFirma.ELECTRONICA, AYER, "Juan Pérez", PNG));

        assertEquals(HttpStatus.BAD_REQUEST, sinArchivo.getStatusCode());
        assertEquals("La carta firmada electrónicamente debe ser un PDF.", foto.getReason());
    }

    @Test
    void soloAceptaPdfJpgOPng() {
        assertEquals("image/png", CartaResponsivaService.validarArchivo(TipoFirma.FISICA, PNG));
        assertEquals("image/jpeg", CartaResponsivaService.validarArchivo(TipoFirma.FISICA, JPG));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> CartaResponsivaService.validarArchivo(TipoFirma.FISICA, "hola".getBytes()));

        assertEquals("El archivo debe ser PDF, JPG o PNG.", error.getReason());
    }

    @Test
    void noAceptaFechaFuturaNiFirmanteVacio() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(cartaGenerada()));
        LocalDateTime manana = LocalDateTime.now(CartaResponsiva.ZONA_HORARIA).plusDays(1);

        assertThrows(ResponseStatusException.class,
                () -> servicio.registrarFirma(RENTA_ID, TipoFirma.FISICA, manana, "Juan Pérez", JPG));
        assertThrows(ResponseStatusException.class,
                () -> servicio.registrarFirma(RENTA_ID, TipoFirma.FISICA, AYER, "  ", JPG));
    }

    @Test
    void noSeFirmaUnaCartaQueNoSeHaGenerado() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrarFirma(RENTA_ID, TipoFirma.FISICA, AYER, "Juan Pérez", JPG));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertFalse(servicio.consultar(RENTA_ID).puedeFirmar());
    }

    @Test
    void unaCartaFirmadaNoSeVuelveAFirmarNiSeEnvia() {
        CartaResponsiva carta = cartaGenerada();
        carta.registrarFirma(TipoFirma.FISICA, AYER, "Juan Pérez", "Juan Pérez", "Auxiliar", JPG, "image/jpeg");
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(carta));

        ResponseStatusException otraVez = assertThrows(ResponseStatusException.class,
                () -> servicio.registrarFirma(RENTA_ID, TipoFirma.ELECTRONICA, AYER, "Otra persona", PDF));
        ResponseStatusException enviar = assertThrows(ResponseStatusException.class,
                () -> servicio.enviar(RENTA_ID, null));

        assertEquals(HttpStatus.CONFLICT, otraVez.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, enviar.getStatusCode());
        assertEquals("Juan Pérez", carta.getFirmante()); // No cambió
    }

    @Test
    void sugiereAlContactoEnLaObraComoFirmante() {
        when(cartas.findByRentaId(RENTA_ID)).thenReturn(Optional.of(cartaGenerada()));

        CartaResponse carta = servicio.consultar(RENTA_ID);

        assertEquals("Juan Pérez", carta.firmanteSugerido());
        assertTrue(carta.puedeFirmar());
        assertNull(carta.firma());
    }

    // Carta generada con los datos actuales de la renta
    private CartaResponsiva cartaGenerada() {
        DatosCarta datos = DatosCarta.de(renta);
        return new CartaResponsiva(renta, ResumenCarta.texto(datos.resumen()), new byte[] { 1 });
    }

    private static Equipo equipo(String numeroUnidad, String serie) {
        Modelo modelo = new Modelo("8FGU25", "Toyota", "COD-" + numeroUnidad, null,
                new BigDecimal("1200.00"), new Categoria("Montacargas"));
        return new Equipo(numeroUnidad, serie, 2022, modelo);
    }
}
