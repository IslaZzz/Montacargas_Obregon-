package mx.montacargasobregon.backend.movil.mantenimientos;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.mantenimientos.modelo.EstadoMantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;
import mx.montacargasobregon.backend.mantenimientos.modelo.TipoMantenimiento;
import mx.montacargasobregon.backend.movil.revisiones.RevisionMecanicoResponse;

/** Un mantenimiento en la lista del mecánico. */
public record MantenimientoMecanicoResponse(Long id, String unidad, String tipoEquipo, TipoMantenimiento tipo,
                                            EstadoMantenimiento estado, LocalDateTime fechaLimite) {

    public static MantenimientoMecanicoResponse de(Mantenimiento m) {
        return new MantenimientoMecanicoResponse(
                m.getId(),
                m.getEquipo().getNumeroUnidad(),
                RevisionMecanicoResponse.tipoEquipo(m.getEquipo()),
                m.getTipo(),
                m.getEstado(),
                m.getFechaLimite());
    }
}