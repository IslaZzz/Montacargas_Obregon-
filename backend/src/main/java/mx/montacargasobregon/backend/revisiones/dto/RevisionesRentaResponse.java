package mx.montacargasobregon.backend.revisiones.dto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.rentas.modelo.Renta;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;

/**
 * Las revisiones de una renta del mismo tipo (todas las de salida, o todas las de regreso).
 * Es cada fila que ve el auxiliar.
 */
public record RevisionesRentaResponse(
        String clave,               // "12-SALIDA": identifica la fila en pantalla
        Long rentaId,
        String folioRenta,
        String cliente,
        String tipo,
        LocalDateTime fechaProgramada,
        String estado,              // Resumen de todas sus revisiones
        long revisadas,
        long total,
        List<RevisionResponse> revisiones) {

    /** Crea la fila a partir de las revisiones de una misma renta y tipo. */
    public static RevisionesRentaResponse deGrupo(List<Revision> grupo) {
        Revision primera = grupo.get(0);
        Renta renta = primera.getEquipoRenta().getRenta();

        // Las canceladas no cuentan para el avance
        List<Revision> activas = grupo.stream()
                .filter(r -> r.getEstado() != EstadoRevision.CANCELADA)
                .toList();

        return new RevisionesRentaResponse(
                renta.getId() + "-" + primera.getTipo(),
                renta.getId(),
                renta.getFolio(),
                nombreCliente(renta.getCliente()),
                primera.getTipo().name(),
                grupo.stream().map(Revision::getFechaProgramada).min(Comparator.naturalOrder()).orElse(null),
                estadoDelGrupo(activas).name(),
                activas.stream().filter(r -> r.getEstado() == EstadoRevision.COMPLETADA).count(),
                activas.size(),
                grupo.stream().map(RevisionResponse::toRevisionResponse).toList());
    }

    /**
     * El estado que se muestra en la fila de la renta:
     * todas terminadas → COMPLETADA; falta asignar alguna → PENDIENTE;
     * alguna empezada o terminada → EN_PROCESO; si no, ASIGNADO.
     */
    static EstadoRevision estadoDelGrupo(List<Revision> activas) {
        if (activas.isEmpty()) {
            return EstadoRevision.CANCELADA;
        }
        if (activas.stream().allMatch(r -> r.getEstado() == EstadoRevision.COMPLETADA)) {
            return EstadoRevision.COMPLETADA;
        }
        if (activas.stream().anyMatch(r -> r.getEstado() == EstadoRevision.PENDIENTE)) {
            return EstadoRevision.PENDIENTE;
        }
        if (activas.stream().anyMatch(r -> r.getEstado() == EstadoRevision.EN_PROCESO
                || r.getEstado() == EstadoRevision.COMPLETADA)) {
            return EstadoRevision.EN_PROCESO;
        }
        return EstadoRevision.ASIGNADO;
    }

    // La empresa si la tiene; si no, el nombre de la persona
    static String nombreCliente(Cliente c) {
        return c.getEmpresa() != null && !c.getEmpresa().isBlank()
                ? c.getEmpresa()
                : c.getNombre() + " " + c.getApellidoPaterno();
    }
}