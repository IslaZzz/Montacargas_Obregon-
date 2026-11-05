package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.clientes.dto.ClienteResponse;
import mx.montacargasobregon.backend.clientes.dto.RazonSocialResponse;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;

/** Detalle completo de una cotización. */
public record CotizacionResponse(
        Long id,
        String folio,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDate fechaEnvio,
        LocalDate fechaRespuesta,
        ClienteResponse cliente,
        RazonSocialResponse razonSocial,
        LocalDateTime fechaEntrega,
        LocalDateTime fechaRecoleccion,
        long dias,
        String direccionObra,
        String contactoObra,
        String telefonoContacto,
        List<PartidaCotizacionResponse> partidas,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total) {

    public static CotizacionResponse toCotizacionResponse(Cotizacion c) {
        return new CotizacionResponse(
                c.getId(),
                c.getFolio(),
                c.getEstado().name(),
                c.getFechaCreacion(),
                c.getFechaEnvio(),
                c.getFechaRespuesta(),
                ClienteResponse.toClienteResponse(c.getCliente()),
                RazonSocialResponse.toRazonSocialResponse(c.getRazonSocial()),
                c.getFechaEntrega(),
                c.getFechaRecoleccion(),
                c.getDias(),
                c.getDireccionObra(),
                c.getContactoObra(),
                c.getTelefonoContacto(),
                c.getPartidas().stream().map(PartidaCotizacionResponse::toPartidaCotizacionResponse).toList(),
                c.getSubtotal(),
                c.getIva(),
                c.getTotal());
    }
}
