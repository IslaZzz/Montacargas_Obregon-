package mx.montacargasobregon.backend.rentas.dto;

import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;

public record EquipoRentaDTO(
    Long id,
    Long equipoId,
    String numeroUnidad,
    String numeroSerie,
    FaseEquipoRenta fase
) {}
