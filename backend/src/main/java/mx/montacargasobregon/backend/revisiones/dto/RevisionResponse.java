package mx.montacargasobregon.backend.revisiones.dto;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.equipos.modelo.Modelo;
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;

/** La revisión de un equipo: cada línea que se ve al desplegar una renta. */
public record RevisionResponse(
        Long id,
        String numeroUnidad,
        String descripcionEquipo,   // "Montacargas Toyota 8FGU25"
        String fase,                // En qué va ese equipo dentro de la renta
        Long mecanicoId,
        String mecanicoNombre,
        String estado,
        String resultado,           // null hasta que se termina
        String notas,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin) {

    public static RevisionResponse toRevisionResponse(Revision r) {
        EquipoRenta er = r.getEquipoRenta();
        Modelo modelo = er.getEquipo().getModelo();
        return new RevisionResponse(
                r.getId(),
                er.getEquipo().getNumeroUnidad(),
                modelo.getCategoria().getNombre() + " " + modelo.getMarca() + " " + modelo.getNombre(),
                er.getFase().name(),
                r.getMecanico() != null ? r.getMecanico().getId() : null,
                r.getMecanico() != null
                        ? r.getMecanico().getNombres() + " " + r.getMecanico().getApellidoPaterno()
                        : null,
                r.getEstado().name(),
                r.getResultado() != null ? r.getResultado().name() : null,
                r.getNotas(),
                r.getFechaInicio(),
                r.getFechaFin());
    }
}