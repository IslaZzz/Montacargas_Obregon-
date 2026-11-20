package mx.montacargasobregon.backend.movil.sesion;

import java.time.Instant;

/**
 * Respuesta de POST /api/movil/sesion. El token solo se entrega aquí; la app lo manda después
 * como "Authorization: Bearer ...". venceEn es un instante UTC (ISO-8601, termina en Z).
 */
public record SesionMovilResponse(String token, Instant venceEn, PersonalActual personal) {
}

