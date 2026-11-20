package mx.montacargasobregon.backend.movil.historial;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.movil.revisiones.RevisionMecanicoResponse;
import mx.montacargasobregon.backend.revisiones.modelo.ResultadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;

/**
 * Un trabajo terminado del mecánico: una revisión o un mantenimiento.
 * tipoRevision y resultado solo vienen en revisiones; tareas, solo en mantenimientos.
 */
public record HistorialMecanicoResponse(Trabajo trabajo, Long id, String unidad, String tipoEquipo,
                                        LocalDateTime terminado,
                                        TipoRevision tipoRevision, ResultadoRevision resultado, Integer tareas) {

    public enum Trabajo {
        REVISION,
        MANTENIMIENTO
    }

    public static HistorialMecanicoResponse de(Revision r) {
        return new HistorialMecanicoResponse(Trabajo.REVISION, r.getId(),
                r.getEquipoRenta().getEquipo().getNumeroUnidad(),
                RevisionMecanicoResponse.tipoEquipo(r.getEquipoRenta().getEquipo()),
                r.getFechaFin(), r.getTipo(), r.getResultado(), null);
    }

    public static HistorialMecanicoResponse de(Mantenimiento m) {
        return new HistorialMecanicoResponse(Trabajo.MANTENIMIENTO, m.getId(), m.getEquipo().getNumeroUnidad(),
                RevisionMecanicoResponse.tipoEquipo(m.getEquipo()), m.getFechaFin(), null, null, m.getTareas().size());
    }
}