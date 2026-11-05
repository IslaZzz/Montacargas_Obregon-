package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;

import mx.montacargasobregon.backend.cotizaciones.modelo.PartidaCotizacion;

/** Un equipo de la cotización con su precio. */
public record PartidaCotizacionResponse(Long id, String unidad, String modelo,
                                        BigDecimal tarifaDia, BigDecimal importe) {

    public static PartidaCotizacionResponse toPartidaCotizacionResponse(PartidaCotizacion p) {
        return new PartidaCotizacionResponse(
                p.getId(),
                p.getEquipo().getNumeroUnidad(),
                p.getEquipo().getModelo().getNombre(),
                p.getTarifaDia(),
                p.getImporte());
    }
}
