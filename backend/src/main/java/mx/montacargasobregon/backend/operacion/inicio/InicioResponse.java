package mx.montacargasobregon.backend.operacion.inicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Lo que muestra la pantalla de inicio del auxiliar. */
public record InicioResponse(
        LocalDate hoy,
        Pendientes pendientes,
        List<Aviso> avisos,            // Del más urgente al menos urgente
        List<Operacion> operaciones) { // Desde hoy, ordenadas por fecha

    /** Lo que queda por hacer hoy. */
    public record Pendientes(int entregas, int recolecciones, int revisiones, int mantenimientos) {
    }

    /** Algo que el auxiliar tiene que resolver. Lleva a la renta si tiene rentaId; si no, a la ruta. */
    public record Aviso(TipoAviso tipo, String titulo, String detalle, Long rentaId, String ruta) {
    }

    /** Un traslado, una revisión o un mantenimiento con fecha. */
    public record Operacion(TipoOperacion tipo, String titulo, String detalle,
                            LocalDateTime desde, LocalDateTime hasta,   // hasta es null si no es un horario
                            Long rentaId, String ruta) {
    }

    public enum TipoAviso {
        TRASLADO_ATRASADO,
        TRASLADO_SIN_OPERADOR,
        ENTREGA_EN_RIESGO,
        MANTENIMIENTO_VENCIDO,
        FALTA_RECOLECCION
    }

    public enum TipoOperacion {
        ENTREGA,
        RECOLECCION,
        REVISION,
        MANTENIMIENTO
    }
}