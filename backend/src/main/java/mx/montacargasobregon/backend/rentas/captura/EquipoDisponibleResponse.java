package mx.montacargasobregon.backend.rentas.captura;

import java.math.BigDecimal;

import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.equipos.modelo.Modelo;

/** Equipo que se puede rentar en las fechas elegidas. */
public record EquipoDisponibleResponse(Long id, String numeroUnidad, String categoria,
                                       String descripcion, BigDecimal tarifaDia) {

    public static EquipoDisponibleResponse deEquipo(Equipo e) {
        Modelo m = e.getModelo();
        return new EquipoDisponibleResponse(
                e.getId(),
                e.getNumeroUnidad(),
                m.getCategoria().getNombre(),
                m.getMarca() + " " + m.getNombre(),
                m.getTarifaDia());
    }
}