package mx.montacargasobregon.backend.personal.dto;

/** Mecánico activo con cuántos equipos tiene por atender, sumando revisiones y mantenimientos. */
public record MecanicoCargaResponse(Long id, String codigo, String nombre, long equiposActivos) {
}