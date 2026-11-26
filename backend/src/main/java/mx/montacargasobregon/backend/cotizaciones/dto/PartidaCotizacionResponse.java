package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;

import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;

/**
 * Un equipo de la cotización con su precio.
 * enOtraCotizacionAceptada: el equipo está en el contrato Apartado de otra cotización, en las mismas fechas (HU-23).
 */
public record PartidaCotizacionResponse(Long id, String unidad, String modelo,
                                        BigDecimal tarifaDia, BigDecimal importe,
                                        boolean enOtraCotizacionAceptada) {

    public static PartidaCotizacionResponse toPartidaCotizacionResponse(EquipoCotizacion p,
                                                                        boolean enOtraCotizacionAceptada) {
        return new PartidaCotizacionResponse(
                p.getId(),
                p.getEquipo().getNumeroUnidad(),
                p.getEquipo().getModelo().getNombre(),
                p.getTarifa(),
                p.getImporte(),
                enOtraCotizacionAceptada);
    }
}
