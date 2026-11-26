package mx.montacargasobregon.backend.cotizaciones.dto;

import mx.montacargasobregon.backend.contratos.modelo.Contrato;

/** El contrato que se generó al aceptar la cotización (HU-23), para mostrarlo y abrirlo desde su detalle. */
public record ContratoDeCotizacionResponse(Long id, String folio, String estado) {

    public static ContratoDeCotizacionResponse toContratoDeCotizacionResponse(Contrato k) {
        return new ContratoDeCotizacionResponse(k.getId(), k.getFolio(), k.getEstado().name());
    }
}
