package mx.montacargasobregon.backend.equipos.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import mx.montacargasobregon.backend.equipos.modelo.Modelo;

/** Modelo con todos sus datos y especificaciones (HU-36). */
public record ModeloDetalleResponse(
        Long id,
        String nombre,
        Long categoriaId,
        String categoria,
        String marca,
        String codigo,
        String marcaModelo,
        String descripcion,
        BigDecimal tarifaDia,
        List<EspecificacionResponse> especificaciones) {

    public static ModeloDetalleResponse toResponse(Modelo m) {
        return new ModeloDetalleResponse(
                m.getId(),
                m.getNombre(),
                m.getCategoria().getId(),
                m.getCategoria().getNombre(),
                m.getMarca(),
                m.getCodigo(),
                m.getMarca() + " " + m.getCodigo(),
                m.getDescripcion(),
                m.getTarifaDia(),
                m.getEspecificaciones().stream()
                        .map(EspecificacionResponse::toResponse)
                        .sorted(Comparator.comparingInt(EspecificacionResponse::orden))
                        .toList());
    }
}
