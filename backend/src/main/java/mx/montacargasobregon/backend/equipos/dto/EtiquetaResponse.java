package mx.montacargasobregon.backend.equipos.dto;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import mx.montacargasobregon.backend.equipos.modelo.Etiqueta;

/** Etiqueta DTO que el backend devuelve al front. */
public record EtiquetaResponse(Long id, String nombre) {

    public static EtiquetaResponse de(Etiqueta e) {
        return new EtiquetaResponse(e.getId(), e.getNombre());
    }

    /** Las etiquetas por orden alfabético, para que siempre salgan igual. */
    public static List<EtiquetaResponse> deTodas(Collection<Etiqueta> etiquetas) {
        return etiquetas.stream()
                .map(EtiquetaResponse::de)
                .sorted(Comparator.comparing(EtiquetaResponse::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
