package mx.montacargasobregon.backend.equipos.consulta;

import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;

/** Un equipo con su estado y, si está en una renta, cuál es. */
public record EquipoEstadoResponse(
        Long id,
        String numeroUnidad,
        String equipo,        // "Toyota 8FGU25"
        String categoria,
        EstadoConsultaEquipo estado,
        Long rentaId,         // Null si no está en una renta
        String rentaFolio) {

    public static EquipoEstadoResponse de(Equipo e, EstadoConsultaEquipo estado, EquipoRenta enRenta) {
        return new EquipoEstadoResponse(
                e.getId(),
                e.getNumeroUnidad(),
                e.getModelo().getMarca() + " " + e.getModelo().getNombre(),
                e.getModelo().getCategoria().getNombre(),
                estado,
                enRenta == null ? null : enRenta.getRenta().getId(),
                enRenta == null ? null : enRenta.getRenta().getFolio());
    }
}