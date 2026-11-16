package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.clientes.dto.ClienteResponse;
import mx.montacargasobregon.backend.clientes.dto.RazonSocialResponse;
import mx.montacargasobregon.backend.comun.dto.DatosDireccionResponse;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

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
        DatosDireccionResponse direccionObra,
        String contactoObra,
        String telefonoContacto,
        List<PartidaCotizacionResponse> partidas,
        BigDecimal trasladoEntrega,
        BigDecimal trasladoRecoleccion,
        Boolean precioDefinido,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        RentaDeCotizacionResponse renta) {

    // Una cotización recién creada todavía no tiene renta
    public static CotizacionResponse toCotizacionResponse(Cotizacion c) {
        return toCotizacionResponse(c, null);
    }

    /** Con la renta que salió de la cotización al aceptarla (HU-37), o null si no tiene. */
    public static CotizacionResponse toCotizacionResponse(Cotizacion c, Renta renta) {
        return new CotizacionResponse(
                c.getId(),
                c.getFolio(),
                c.getEstado().name(),
                c.getFechaHoraCreacion(),
                c.getFechaHoraEnvio() == null ? null : c.getFechaHoraEnvio().toLocalDate(),
                c.getFechaRespuesta(),
                ClienteResponse.toClienteResponse(c.getCliente()),
                RazonSocialResponse.toRazonSocialResponse(c.getRazonSocial()),
                c.getFechaHoraEntrega(),
                c.getFechaHoraRecoleccion(),
                c.getDias(),
                DatosDireccionResponse.toDatosDireccionResponse(c.getDireccionObra()),
                c.getContactoObra().getNombre(),
                c.getContactoObra().getTelefono(),
                c.getEquipos().stream().map(PartidaCotizacionResponse::toPartidaCotizacionResponse).toList(),
                c.getTrasladoEntrega(),
                c.getTrasladoRecoleccion(),
                c.isPrecioDefinido(),
                c.getSubtotal(),
                c.getIva(),
                c.getTotal(),
                renta == null ? null : RentaDeCotizacionResponse.toRentaDeCotizacionResponse(renta));
    }
}