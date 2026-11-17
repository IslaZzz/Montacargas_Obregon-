package mx.montacargasobregon.backend.equipos.dto;

import java.util.List;

import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;
import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;

/** Campo de especificación que pide una categoría. */
public record DefinicionCampoResponse(Long id, String nombre, TipoCampo tipo, String unidad,
                                      boolean requerido, int orden, List<String> opciones) {

    public static DefinicionCampoResponse toResponse(DefinicionCampo c) {
        return new DefinicionCampoResponse(c.getId(), c.getNombre(), c.getTipo(), c.getUnidad(),
                c.isRequerido(), c.getOrden(), List.copyOf(c.getOpciones()));
    }
}
