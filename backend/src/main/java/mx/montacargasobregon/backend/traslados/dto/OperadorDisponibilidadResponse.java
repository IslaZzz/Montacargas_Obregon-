package mx.montacargasobregon.backend.traslados.dto;

public record OperadorDisponibilidadResponse(
        Long id,
        String codigo,
        String nombreCompleto,
        String iniciales,
        boolean disponible,
        String motivo,
        Long conflictoTrasladoId,
        String conflictoTrasladoFolio
) {
}

