package mx.montacargasobregon.backend.movil.revisiones;

import java.util.List;

import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;
import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;

/** Un campo que el mecánico llena en la revisión, según la categoría del equipo. */
public record CampoRevisionResponse(Long id, String nombre, TipoCampo tipo, String unidad, boolean requerido,
                                    List<String> opciones) {

    public static CampoRevisionResponse de(DefinicionCampo c) {
        return new CampoRevisionResponse(c.getId(), c.getNombre(), c.getTipo(), c.getUnidad(), c.isRequerido(),
                List.copyOf(c.getOpciones()));
    }
}