package mx.montacargasobregon.backend.movil.revisiones;

import mx.montacargasobregon.backend.revisiones.modelo.ResultadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;

/** Cómo quedó la revisión al terminarla, para la pantalla de cierre. */
public record RevisionTerminadaResponse(Long id, String unidad, TipoRevision tipo, ResultadoRevision resultado) {
}