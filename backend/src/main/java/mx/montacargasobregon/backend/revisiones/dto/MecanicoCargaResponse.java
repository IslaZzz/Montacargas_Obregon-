package mx.montacargasobregon.backend.revisiones.dto;

/** Mecánico activo con cuántas revisiones tiene sin terminar. */
public record MecanicoCargaResponse(Long id, String codigo, String nombre, long equiposActivos) {
}