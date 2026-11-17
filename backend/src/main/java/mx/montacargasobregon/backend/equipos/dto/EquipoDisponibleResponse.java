package mx.montacargasobregon.backend.equipos.dto;

import java.math.BigDecimal;
import java.util.List;

public record EquipoDisponibleResponse(
        Long id, String numeroUnidad, String modelo, List<EtiquetaResponse> etiquetas, BigDecimal tarifaDia) {
}
