package mx.montacargasobregon.backend.equipos.dto;

import java.math.BigDecimal;

import mx.montacargasobregon.backend.equipos.modelo.Modelo;

/** Tarifa DTO de respuesta que el backend devuelve al front. */
public record TarifaResponse(
        Long modeloId,
        String nombre,
        String marca,
        String codigo,
        Long categoriaId,
        String categoria,
        BigDecimal tarifaDia) {

    public static TarifaResponse toTarifaResponse(Modelo m) {
        return new TarifaResponse(
                m.getId(),
                m.getNombre(),
                m.getMarca(),
                m.getCodigo(),
                m.getCategoria().getId(),
                m.getCategoria().getNombre(),
                m.getTarifaDia());
    }
}