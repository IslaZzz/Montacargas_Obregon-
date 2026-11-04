package mx.montacargasobregon.backend.equipos.dto;

import java.math.BigDecimal;

import mx.montacargasobregon.backend.equipos.modelo.Modelo;

/** Modelo DTO que el backend devuelve al front. */
public record ModeloResponse(Long id, String nombre, String marca, String codigo,
                             BigDecimal tarifaDia, String categoria) {

    public static ModeloResponse toModeloResponse(Modelo m) {
        return new ModeloResponse(
                m.getId(), m.getNombre(), m.getMarca(), m.getCodigo(),
                m.getTarifaDia(), m.getCategoria().getNombre());
    }
}