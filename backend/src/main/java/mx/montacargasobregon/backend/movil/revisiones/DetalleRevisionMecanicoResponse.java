package mx.montacargasobregon.backend.movil.revisiones;

import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.equipos.dto.EspecificacionResponse;
import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.ResultadoRevision;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.TipoRevision;

/**
 * Una revisión con lo necesario para hacerla (la ficha del modelo y los campos de revisión de su categoría)
 * o, si ya terminó, con lo que se anotó.
 */
public record DetalleRevisionMecanicoResponse(Long id, String unidad, String tipoEquipo, String modelo,
                                              List<EspecificacionResponse> especificaciones,
                                              TipoRevision tipo, EstadoRevision estado,
                                              LocalDateTime fechaProgramada, LocalDateTime fechaFin,
                                              String rentaFolio, List<CampoRevisionResponse> campos,
                                              List<ValorRevisionResponse> valores,
                                              ResultadoRevision resultado, String notas) {

    public static DetalleRevisionMecanicoResponse de(Revision revision, List<DefinicionCampo> campos) {
        Equipo equipo = revision.getEquipoRenta().getEquipo();
        return new DetalleRevisionMecanicoResponse(
                revision.getId(),
                equipo.getNumeroUnidad(),
                RevisionMecanicoResponse.tipoEquipo(equipo),
                equipo.getModelo() == null ? null : equipo.getModelo().getNombre(),
                RevisionMecanicoResponse.especificaciones(equipo),
                revision.getTipo(),
                revision.getEstado(),
                revision.getFechaProgramada(),
                revision.getFechaFin(),
                revision.getEquipoRenta().getRenta().getFolio(),
                campos.stream().map(CampoRevisionResponse::de).toList(),
                revision.getValores().stream().map(ValorRevisionResponse::de).toList(),
                revision.getResultado(),
                revision.getNotas());
    }
}