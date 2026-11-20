package mx.montacargasobregon.backend.movil.mantenimientos;

import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.equipos.dto.EspecificacionResponse;
import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;
import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.TipoMantenimiento;
import mx.montacargasobregon.backend.movil.revisiones.RevisionMecanicoResponse;
import mx.montacargasobregon.backend.movil.revisiones.ValorRevisionResponse;

/**
 * Un mantenimiento con sus tareas; sirve para trabajarlo y para el resumen del historial.
 * Si salió de una revisión, trae también lo que se anotó en ella (sin las fotos).
 */
public record DetalleMantenimientoMecanicoResponse(Long id, String unidad, String tipoEquipo, String modelo,
                                                   List<EspecificacionResponse> especificaciones,
                                                   TipoMantenimiento tipo, EstadoMantenimiento estado,
                                                   String descripcionFalla, LocalDateTime fechaLimite,
                                                   LocalDateTime fechaFin, List<ValorRevisionResponse> revision,
                                                   List<TareaMecanicoResponse> tareas, int atendidas,
                                                   boolean puedeFinalizar, List<String> fotosPorTarea) {

    public static DetalleMantenimientoMecanicoResponse de(Mantenimiento m, List<String> fotosPorTarea) {
        List<TareaMecanicoResponse> tareas = m.getTareas().stream().map(TareaMecanicoResponse::de).toList();
        boolean abierto = m.getEstado() == EstadoMantenimiento.ASIGNADO || m.getEstado() == EstadoMantenimiento.EN_PROCESO;
        return new DetalleMantenimientoMecanicoResponse(
                m.getId(),
                m.getEquipo().getNumeroUnidad(),
                RevisionMecanicoResponse.tipoEquipo(m.getEquipo()),
                m.getEquipo().getModelo() == null ? null : m.getEquipo().getModelo().getNombre(),
                RevisionMecanicoResponse.especificaciones(m.getEquipo()),
                m.getTipo(),
                m.getEstado(),
                m.getDescripcionFalla(),
                m.getFechaLimite(),
                m.getFechaFin(),
                m.getRevision() == null ? List.of() : m.getRevision().getValores().stream()
                        .filter(v -> v.getTipo() != TipoCampo.FOTO)
                        .map(ValorRevisionResponse::de)
                        .toList(),
                tareas,
                (int) m.getTareas().stream().filter(t -> t.atendida()).count(),
                abierto && m.tareasCompletas(),
                fotosPorTarea);
    }
}