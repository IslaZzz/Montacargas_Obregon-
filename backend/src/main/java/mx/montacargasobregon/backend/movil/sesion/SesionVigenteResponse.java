package mx.montacargasobregon.backend.movil.sesion;

import java.time.Instant;

/** Respuesta de GET /api/movil/yo: quién es y hasta cuándo dura su sesión (sin devolver el token). */
public record SesionVigenteResponse(Instant venceEn, PersonalActual personal) {
}

