package mx.montacargasobregon.backend.equipos.dto;

import java.util.List;

import mx.montacargasobregon.backend.equipos.modelo.Equipo;

/** Equipo DTO de respuesta que el backend devuelve al front. */
public record EquipoResponse(Long id, String numeroUnidad, String numeroSerie,
                             Integer anioCompra, String estado, List<EtiquetaResponse> etiquetas, ModeloResponse modelo) {

    public static EquipoResponse toEquipoResponse(Equipo e) {
        return new EquipoResponse(
                e.getId(),
                e.getNumeroUnidad(),
                e.getNumeroSerie(),
                e.getAnioCompra(),
                e.getEstado().name(),
                EtiquetaResponse.deTodas(e.getEtiquetas()),
                ModeloResponse.toModeloResponse(e.getModelo()));
    }
}
