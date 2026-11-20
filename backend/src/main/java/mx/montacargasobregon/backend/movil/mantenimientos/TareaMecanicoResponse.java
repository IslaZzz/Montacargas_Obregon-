package mx.montacargasobregon.backend.movil.mantenimientos;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoTarea;
import mx.montacargasobregon.backend.mantenimientos.modelo.OrigenTarea;
import mx.montacargasobregon.backend.mantenimientos.modelo.TareaMantenimiento;

/** Una tarea del mantenimiento con lo que se registró al atenderla. */
public record TareaMecanicoResponse(Long id, String titulo, String hallazgo, OrigenTarea origen, String componente,
                                    String accionSugerida, EstadoTarea estado, String trabajoRealizado,
                                    String refacciones, String tiempoInvertido, String observaciones,
                                    LocalDateTime atendidaEn, String incidencia) {

    public static TareaMecanicoResponse de(TareaMantenimiento t) {
        return new TareaMecanicoResponse(t.getId(), t.getTitulo(), t.getHallazgo(), t.getOrigen(), t.getComponente(),
                t.getAccionSugerida(), t.getEstado(), t.getTrabajoRealizado(), t.getRefacciones(),
                t.getTiempoInvertido(), t.getObservaciones(), t.getAtendidaEn(), t.getIncidencia());
    }
}