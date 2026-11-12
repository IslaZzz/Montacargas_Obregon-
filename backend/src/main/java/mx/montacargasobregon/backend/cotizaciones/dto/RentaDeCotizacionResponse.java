package mx.montacargasobregon.backend.cotizaciones.dto;

import mx.montacargasobregon.backend.rentas.modelo.Renta;

/** La renta que salió de una cotización aceptada (HU-37), para mostrarla en su detalle. */
public record RentaDeCotizacionResponse(String folio, String estado) {

    public static RentaDeCotizacionResponse toRentaDeCotizacionResponse(Renta r) {
        return new RentaDeCotizacionResponse(r.getFolio(), r.getEstado().name());
    }
}
