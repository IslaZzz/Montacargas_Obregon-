package mx.montacargasobregon.backend.movil.sesion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.modelo.TipoPersonal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

@ExtendWith(MockitoExtension.class)
class SesionMovilServiceTest {

    private static final Instant INICIO = Instant.parse("2026-11-20T06:00:00Z");

    @Mock
    private SesionMovilRepository sesiones;
    @Mock
    private PersonalRepository personalRepositorio;

    // Las "tablas" de mentira: guardan lo que el servicio guarda
    private final List<SesionMovil> guardadas = new ArrayList<>();
    private Personal operador;
    private Personal mecanico;

    @BeforeEach
    void setUp() {
        operador = personal(1L, "OPT-10", "Luis", TipoPersonal.OP_TRASLADO);
        mecanico = personal(2L, "MEC-10", "Rafael", TipoPersonal.MECANICO);

        lenient().when(personalRepositorio.findByCodigoIgnoreCaseAndActivoTrue(anyString())).thenAnswer(inv -> {
            String codigo = inv.getArgument(0);
            return List.of(operador, mecanico).stream()
                    .filter(p -> p.isActivo() && p.getCodigo().equalsIgnoreCase(codigo))
                    .findFirst();
        });
        lenient().when(sesiones.save(any(SesionMovil.class))).thenAnswer(inv -> {
            SesionMovil s = inv.getArgument(0);
            guardadas.add(s);
            return s;
        });
        lenient().when(sesiones.buscarPorTokenHash(anyString())).thenAnswer(inv -> guardadas.stream()
                .filter(s -> s.getTokenHash().equals(inv.getArgument(0)))
                .findFirst());
        lenient().when(sesiones.revocarActivasDe(anyLong())).thenAnswer(inv -> {
            long personalId = inv.getArgument(0);
            int cuantas = 0;
            for (SesionMovil s : guardadas) {
                if (s.getPersonal().getId() == personalId && !s.isRevocada()) {
                    s.revocar();
                    cuantas++;
                }
            }
            return cuantas;
        });
    }

    @Test
    void entrarConCodigoDeOperadorGuardaSoloElHashYVenceEn22Horas() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("OPT-10");

        assertEquals(INICIO.plus(Duration.ofHours(22)), respuesta.venceEn());
        assertEquals(1L, respuesta.personal().id());
        assertEquals(TipoPersonal.OP_TRASLADO, respuesta.personal().tipo());
        assertEquals(43, respuesta.token().length()); // 32 bytes en Base64 sin relleno

        assertEquals(1, guardadas.size());
        SesionMovil guardada = guardadas.get(0);
        assertEquals(64, guardada.getTokenHash().length());
        assertEquals(SesionMovilService.sha256Hex(respuesta.token()), guardada.getTokenHash());
        assertNotEquals(respuesta.token(), guardada.getTokenHash());
        assertEquals(INICIO, guardada.getCreadoEn());
        assertEquals(INICIO.plus(Duration.ofHours(22)), guardada.getVenceEn());
        assertFalse(guardada.isRevocada());
    }

    @Test
    void entrarConCodigoDeMecanicoFunciona() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("  mec-10 ");

        assertEquals(TipoPersonal.MECANICO, respuesta.personal().tipo());
        assertEquals(INICIO.plus(Duration.ofHours(22)), respuesta.venceEn());
    }

    @Test
    void codigoInexistenteResponde401() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicioEn(INICIO).entrar("NO-EXISTE"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("No encontramos ese código.", error.getReason());
        assertTrue(guardadas.isEmpty());
    }

    @Test
    void personalInactivoNoPuedeEntrar() {
        operador.desactivar();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicioEn(INICIO).entrar("OPT-10"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("No encontramos ese código.", error.getReason());
    }

    @Test
    void tokenValidoUnSegundoAntesDeLas22HorasSirveYNoExtiendeLaVigencia() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("OPT-10");
        Instant casiVencida = INICIO.plus(Duration.ofHours(22)).minusSeconds(1); // 21 h 59 min 59 s

        SesionVigenteResponse vigente = servicioEn(casiVencida).validarSesion(respuesta.token());

        assertEquals(1L, vigente.personal().id());
        assertEquals(respuesta.venceEn(), vigente.venceEn());
        assertEquals(INICIO.plus(Duration.ofHours(22)), guardadas.get(0).getVenceEn());
        assertEquals(1L, servicioEn(casiVencida).validarToken(respuesta.token()).id());
    }

    @Test
    void tokenExactamenteALas22HorasYaNoSirve() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("OPT-10");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicioEn(INICIO.plus(Duration.ofHours(22))).validarToken(respuesta.token()));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertTrue(error.getReason().contains("venció"), error.getReason());
    }

    @Test
    void tokenA22HorasConUnSegundoYaNoSirve() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("MEC-10");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicioEn(INICIO.plus(Duration.ofHours(22)).plusSeconds(1)).validarSesion(respuesta.token()));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertTrue(error.getReason().contains("venció"), error.getReason());
    }

    @Test
    void nuevoLoginDelMismoOperadorRevocaLaSesionAnterior() {
        SesionMovilResponse primera = servicioEn(INICIO).entrar("OPT-10");
        SesionMovilResponse segunda = servicioEn(INICIO.plusSeconds(60)).entrar("OPT-10");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicioEn(INICIO.plusSeconds(120)).validarToken(primera.token()));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertTrue(error.getReason().contains("cerrada o reemplazada"), error.getReason());

        assertEquals(1L, servicioEn(INICIO.plusSeconds(120)).validarToken(segunda.token()).id());
        // La vigencia de la nueva cuenta desde su propio login, no desde la primera
        assertEquals(INICIO.plusSeconds(60).plus(Duration.ofHours(22)), segunda.venceEn());
    }

    @Test
    void otroOperadorNoAfectaLaSesionDeQuienYaEntro() {
        SesionMovilResponse deOperador = servicioEn(INICIO).entrar("OPT-10");
        servicioEn(INICIO.plusSeconds(10)).entrar("MEC-10");

        assertEquals(1L, servicioEn(INICIO.plusSeconds(20)).validarToken(deOperador.token()).id());
    }

    @Test
    void personalDesactivadoConTokenVigenteResponde401YSeRevoca() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("OPT-10");
        operador.desactivar();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicioEn(INICIO.plusSeconds(30)).validarToken(respuesta.token()));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals("Tu cuenta no está activa.", error.getReason());
        assertTrue(guardadas.get(0).isRevocada());
    }

    @Test
    void cerrarSesionRevocaElTokenYNoSePuedeReutilizar() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("OPT-10");

        servicioEn(INICIO.plusSeconds(5)).cerrarSesion(respuesta.token());

        assertTrue(guardadas.get(0).isRevocada());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicioEn(INICIO.plusSeconds(6)).validarToken(respuesta.token()));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void cerrarSesionEsIdempotente() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("OPT-10");
        SesionMovilService servicio = servicioEn(INICIO.plusSeconds(5));

        servicio.cerrarSesion(respuesta.token());
        servicio.cerrarSesion(respuesta.token()); // ya revocada
        servicio.cerrarSesion("token-que-no-existe");
        servicio.cerrarSesion(null);
        servicio.cerrarSesion("  ");

        assertTrue(guardadas.get(0).isRevocada());
    }

    @Test
    void cerrarSesionTambienFuncionaConSesionVencida() {
        SesionMovilResponse respuesta = servicioEn(INICIO).entrar("OPT-10");

        servicioEn(INICIO.plus(Duration.ofHours(30))).cerrarSesion(respuesta.token());

        assertTrue(guardadas.get(0).isRevocada());
    }

    @Test
    void tokenFaltanteVacioOInventadoResponde401() {
        SesionMovilService servicio = servicioEn(INICIO);

        for (String token : new String[] {null, "", "   ", "inventado"}) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> servicio.validarToken(token));
            assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        }
    }

    @Test
    void tokenDeLeeSoloElFormatoBearer() {
        assertEquals("abc", SesionMovilService.tokenDe("Bearer abc"));
        assertEquals("abc", SesionMovilService.tokenDe("bearer   abc "));
        assertNull(SesionMovilService.tokenDe(null));
        assertNull(SesionMovilService.tokenDe(""));
        assertNull(SesionMovilService.tokenDe("Bearer"));
        assertNull(SesionMovilService.tokenDe("Bearer "));
        assertNull(SesionMovilService.tokenDe("Basic abc"));
        assertNull(SesionMovilService.tokenDe("7"));
    }

    @Test
    void laDuracionSeTomaDeLaConfiguracion() {
        SesionMovilService de8Horas = new SesionMovilService(sesiones, personalRepositorio, reloj(INICIO), 8);

        SesionMovilResponse respuesta = de8Horas.entrar("OPT-10");

        assertEquals(INICIO.plus(Duration.ofHours(8)), respuesta.venceEn());
    }

    @Test
    void unaDuracionInvalidaNoSePermite() {
        assertThrows(IllegalArgumentException.class,
                () -> new SesionMovilService(sesiones, personalRepositorio, reloj(INICIO), 0));
    }

    // ---- apoyo ----

    private SesionMovilService servicioEn(Instant ahora) {
        return new SesionMovilService(sesiones, personalRepositorio, reloj(ahora), 22);
    }

    private static Clock reloj(Instant ahora) {
        return Clock.fixed(ahora, ZoneOffset.UTC);
    }

    private static Personal personal(Long id, String codigo, String nombre, TipoPersonal tipo) {
        Personal p = new Personal(codigo, nombre, "Prueba", null, tipo);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }
}

