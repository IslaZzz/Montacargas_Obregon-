package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;

import mx.montacargasobregon.backend.cotizaciones.modelo.EquipoCotizacion;

/** Un equipo de la cotización con su precio. */
public record PartidaCotizacionResponse(Long id, String unidad, String modelo,
                                        BigDecimal tarifaDia, BigDecimal importe) {

    public static PartidaCotizacionResponse toPartidaCotizacionResponse(EquipoCotizacion p) {
        return new PartidaCotizacionResponse(
                p.getId(),
                p.getEquipo().getNumeroUnidad(),
                p.getEquipo().getModelo().getNombre(),
                p.getTarifa(),
                p.getImporte());
    }
}
