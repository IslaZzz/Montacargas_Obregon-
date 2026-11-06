package mx.montacargasobregon.backend.mantenimientos.dto;

/** Mecánico activo con cuántos mantenimientos tiene sin terminar. */
public record MecanicoCargaResponse(Long id, String codigo, String nombre, long equiposActivos) {
}