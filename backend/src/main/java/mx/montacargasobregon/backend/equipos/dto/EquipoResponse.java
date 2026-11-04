package mx.montacargasobregon.backend.equipos.dto;

import java.util.Set;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;

/** Equipo DTO de respuesta que el backend devuelve al front. */
public record EquipoResponse(Long id, String numeroUnidad, String numeroSerie,
                             Integer anioCompra, String estado, Set<Etiqueta> etiquetas, ModeloResponse modelo) {

    public static EquipoResponse toEquipoResponse(Equipo e) {
        return new EquipoResponse(
                e.getId(),
                e.getNumeroUnidad(),
                e.getNumeroSerie(),
                e.getAnioCompra(),
                e.getEstado().name(),
                e.getEtiquetas(),
                ModeloResponse.toModeloResponse(e.getModelo()));
    }
}