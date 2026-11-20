package mx.montacargasobregon.backend.movil.sesion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.personal.repositorio.PersonalRepository;

/**
 * Sesiones de la app móvil: entrar con el código del gafete, validar el token de cada petición
 * y cerrar sesión. La sesión dura una jornada fija (app.movil.sesion.duracion-horas) contada desde
 * que se entra, y solo hay una activa por persona. El servidor decide la vigencia, con Instant y un Clock.
 */
@Service
public class SesionMovilService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_DEL_TOKEN = 32;
    private static final String PREFIJO = "bearer ";

    private final SesionMovilRepository sesiones;
    private final PersonalRepository personalRepositorio;
    private final Clock reloj;
    private final Duration duracion;

    public SesionMovilService(SesionMovilRepository sesiones,
                              PersonalRepository personalRepositorio,
                              Clock reloj,
                              @Value("${app.movil.sesion.duracion-horas:22}") long duracionHoras) {
        if (duracionHoras <= 0) {
            throw new IllegalArgumentException("app.movil.sesion.duracion-horas debe ser mayor a 0");
        }
        this.sesiones = sesiones;
        this.personalRepositorio = personalRepositorio;
        this.reloj = reloj;
        this.duracion = Duration.ofHours(duracionHoras);
    }

    /**
     * Entra con el código: cierra las sesiones anteriores de esa persona y abre una nueva.
     * 401 si el código no existe o la persona está de baja (mismo mensaje en ambos casos).
     */
    @Transactional
    public SesionMovilResponse entrar(String codigo) {
        String limpio = codigo == null ? "" : codigo.trim();
        Personal personal = personalRepositorio.findByCodigoIgnoreCaseAndActivoTrue(limpio)
                .orElseThrow(() -> noAutorizado("No encontramos ese código."));

        sesiones.revocarActivasDe(personal.getId());

        Instant ahora = Instant.now(reloj);
        String token = nuevoToken();
        SesionMovil sesion = new SesionMovil(personal, sha256Hex(token), ahora, ahora.plus(duracion));
        sesiones.save(sesion);

        return new SesionMovilResponse(token, sesion.getVenceEn(), PersonalActual.de(personal));
    }

    /** Quién es el dueño del token. Lanza 401 si no sirve. Es lo que usa PersonalActualResolver. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public PersonalActual validarToken(String token) {
        return validarSesion(token).personal();
    }

    /**
     * Valida el token y regresa a su dueño con la hora en que vence. No extiende la vigencia.
     * Si la persona ya está de baja, además se revoca la sesión (por eso no se revierte la
     * transacción cuando se lanza el 401).
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public SesionVigenteResponse validarSesion(String token) {
        if (token == null || token.isBlank()) {
            throw noAutorizado("Inicia sesión para continuar.");
        }

        SesionMovil sesion = sesiones.buscarPorTokenHash(sha256Hex(token.trim()))
                .orElseThrow(() -> noAutorizado(MENSAJE_CERRADA));
        if (sesion.isRevocada()) {
            throw noAutorizado(MENSAJE_CERRADA);
        }

        Personal personal = sesion.getPersonal();
        if (!personal.isActivo()) {
            sesion.revocar();
            throw noAutorizado("Tu cuenta no está activa.");
        }

        // Tope duro: a la hora exacta de vencimiento ya no sirve
        if (!Instant.now(reloj).isBefore(sesion.getVenceEn())) {
            throw noAutorizado("Tu sesión de " + duracion.toHours()
                    + " horas venció. Ingresa tu código para iniciar una nueva jornada.");
        }

        return new SesionVigenteResponse(sesion.getVenceEn(), PersonalActual.de(personal));
    }

    /** Cierra la sesión del token. No falla si ya estaba cerrada, vencida o no existe. */
    @Transactional
    public void cerrarSesion(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sesiones.buscarPorTokenHash(sha256Hex(token.trim())).ifPresent(sesion -> {
            if (!sesion.isRevocada()) {
                sesion.revocar();
            }
        });
    }

    /** El token de un encabezado "Authorization: Bearer xxx"; null si no viene con ese formato. */
    public static String tokenDe(String autorizacion) {
        if (autorizacion == null) {
            return null;
        }
        String limpio = autorizacion.trim();
        if (limpio.length() <= PREFIJO.length() || !limpio.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            return null;
        }
        String token = limpio.substring(PREFIJO.length()).trim();
        return token.isEmpty() ? null : token;
    }

    // ---- apoyo ----

    private static final String MENSAJE_CERRADA =
            "Tu sesión fue cerrada o reemplazada. Ingresa tu código nuevamente.";

    /** 32 bytes aleatorios en Base64 sin relleno, seguro para ir en un encabezado. */
    private static String nuevoToken() {
        byte[] bytes = new byte[BYTES_DEL_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: lo único que se guarda del token. */
    static String sha256Hex(String texto) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    private static ResponseStatusException noAutorizado(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, mensaje);
    }
}

