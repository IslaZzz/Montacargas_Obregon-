package mx.montacargasobregon.backend.equipos.dto;

import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;
import mx.montacargasobregon.backend.equipos.modelo.ValorEspecificacion;

/** Valor de un modelo en un campo de su categoría, con el nombre y la unidad del campo. */
public record EspecificacionResponse(Long campoId, String nombre, TipoCampo tipo, String unidad,
                                     int orden, String valor) {

    public static EspecificacionResponse toResponse(ValorEspecificacion v) {
        return new EspecificacionResponse(v.getCampo().getId(), v.getCampo().getNombre(),
                v.getCampo().getTipo(), v.getCampo().getUnidad(), v.getCampo().getOrden(), v.getValor());
    }
}
