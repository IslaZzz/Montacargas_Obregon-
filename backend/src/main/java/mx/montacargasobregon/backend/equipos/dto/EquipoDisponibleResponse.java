package mx.montacargasobregon.backend.equipos.dto;

import java.math.BigDecimal;
import java.util.Set;

import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;

public record EquipoDisponibleResponse(
        Long id, String numeroUnidad, String modelo, Set<Etiqueta> etiquetas, BigDecimal tarifaDia) {
}
