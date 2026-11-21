package mx.montacargasobregon.backend.movil.revisiones;

import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoInspeccion;
import mx.montacargasobregon.backend.revisiones.modelo.ValorRevision;

/** Lo que se anotó en un campo de la revisión. Lo usan el resumen de la revisión y el mantenimiento. */
public record ValorRevisionResponse(String nombre, TipoCampo tipo, String unidad, String valor,
                                    EstadoInspeccion estado, String detalle) {

    public static ValorRevisionResponse de(ValorRevision v) {
        return new ValorRevisionResponse(v.getNombre(), v.getTipo(), v.getUnidad(), v.getValor(), v.getEstado(),
                v.getDetalle());
    }
}