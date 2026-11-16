package mx.montacargasobregon.backend.movil.traslados;

import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;
import mx.montacargasobregon.backend.rentas.modelo.FaseEquipoRenta;

/** Un equipo de la renta, como lo ve el operador en su tarea. */
public record EquipoTareaResponse(Long id, String numeroUnidad, FaseEquipoRenta fase, boolean listo) {

    public static EquipoTareaResponse de(EquipoRenta equipoRenta, FaseEquipoRenta faseRequerida) {
        FaseEquipoRenta fase = equipoRenta.getFase();
        return new EquipoTareaResponse(
                equipoRenta.getId(),
                equipoRenta.getEquipo().getNumeroUnidad(),
                fase,
                fase == faseRequerida || fase == FaseEquipoRenta.EN_TRASLADO);
    }
}