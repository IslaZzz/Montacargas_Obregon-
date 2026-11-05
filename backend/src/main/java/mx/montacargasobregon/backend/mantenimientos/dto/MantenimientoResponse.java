package mx.montacargasobregon.backend.mantenimientos.dto;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.comun.Persona;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.mantenimientos.modelo.Mantenimiento;

/** Mantenimiento DTO de respuesta: lo que muestra cada fila de la tabla. */
public record MantenimientoResponse(
        Long id,
        String numeroUnidad,
        String descripcionEquipo, // Por ejemplo,"Montacargas Toyota 8FGU25"
        String tipo,
        String descripcionFalla,
        String reportadoPor,
        LocalDateTime fechaReporte,
        LocalDateTime fechaLimite,
        String folioRenta, // Siempre null por ahora: se llenará cuando exista Renta
        Long mecanicoId,
        String mecanicoNombre,
        String estado) {

    public static MantenimientoResponse toMantenimientoResponse(Mantenimiento m) {
        Modelo modelo = m.getEquipo().getModelo();
        return new MantenimientoResponse(
                m.getId(),
                m.getEquipo().getNumeroUnidad(),
                modelo.getCategoria().getNombre() + " " + modelo.getMarca() + " " + modelo.getNombre(),
                m.getTipo().name(),
                m.getDescripcionFalla(),
                nombreCorto(m.getReportadoPor()),
                m.getFechaReporte(),
                m.getFechaLimite(),
                null,
                m.getMecanico() != null ? m.getMecanico().getId() : null,
                nombreCorto(m.getMecanico()),
                m.getEstado().name());
    }

    // "Rafael Amaya": sin apellido materno para que quepa en la tabla
    static String nombreCorto(Persona p) {
        return p == null ? null : p.getNombres() + " " + p.getApellidoPaterno();
    }
}