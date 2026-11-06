package mx.montacargasobregon.backend.personal.dto;

/** Resultado de contar, en la base de datos, los trabajos sin terminar de cada mecánico. */
public record CargaPorMecanico(Long mecanicoId, Long total) {
}