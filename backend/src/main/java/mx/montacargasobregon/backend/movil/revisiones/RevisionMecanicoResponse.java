package mx.montacargasobregon.backend.movil.revisiones;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import mx.montacargasobregon.backend.equipos.dto.EspecificacionResponse;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;

/** Una revisión en la lista "Equipos por revisar" del mecánico. */
public record RevisionMecanicoResponse(Long id, String unidad, String tipoEquipo, TipoRevision tipo,
                                       EstadoRevision estado, LocalDateTime fechaProgramada, String rentaFolio) {

    public static RevisionMecanicoResponse de(Revision revision) {
        Equipo equipo = revision.getEquipoRenta().getEquipo();
        return new RevisionMecanicoResponse(
                revision.getId(),
                equipo.getNumeroUnidad(),
                tipoEquipo(equipo),
                revision.getTipo(),
                revision.getEstado(),
                revision.getFechaProgramada(),
                revision.getEquipoRenta().getRenta().getFolio());
    }

    /** "Montacargas", "Apilador": la categoría del modelo. */
    public static String tipoEquipo(Equipo equipo) {
        return equipo.getModelo() == null ? null : equipo.getModelo().getCategoria().getNombre();
    }

    /** Las especificaciones de su modelo (capacidad, tipo de energía...), en el orden de su categoría. */
    public static List<EspecificacionResponse> especificaciones(Equipo equipo) {
        if (equipo.getModelo() == null) {
            return List.of();
        }
        return equipo.getModelo().getEspecificaciones().stream()
                .map(EspecificacionResponse::toResponse)
                .sorted(Comparator.comparingInt(EspecificacionResponse::orden))
                .toList();
    }
}