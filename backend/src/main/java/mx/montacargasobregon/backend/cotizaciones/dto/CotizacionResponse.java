package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.clientes.dto.ClienteResponse;
import mx.montacargasobregon.backend.clientes.dto.RazonSocialResponse;
import mx.montacargasobregon.backend.comun.dto.DatosDireccionResponse;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Descuento;
import mx.montacargasobregon.backend.cotizaciones.modelo.TipoDescuento;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/** Detalle completo de una cotización. */
public record CotizacionResponse(
        Long id,
        String folio,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDate fechaEnvio,
        LocalDate fechaRespuesta,
        LocalDate validaHasta, // Último día para que el cliente la acepte
        int diasVigencia,
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
        BigDecimal subtotal, // Equipos + traslados, antes del descuento
        // Descuento: tipo es NINGUNO si no hay; valor y motivo vienen en null en ese caso
        String descuentoTipo,
        BigDecimal descuentoValor,
        String descuentoMotivo,
        BigDecimal descuentoMonto, // Pesos que se descuentan; 0 si no hay
        BigDecimal iva,
        BigDecimal total,
        // Solicitud de autorización del descuento y la respuesta de gerencia; null si no hay descuento
        AutorizacionDescuentoResponse autorizacion,
        RentaDeCotizacionResponse renta) {

    // Una cotización recién creada todavía no tiene renta
    public static CotizacionResponse toCotizacionResponse(Cotizacion c) {
        return toCotizacionResponse(c, null);
    }

    /** Con la renta que salió de la cotización al aceptarla, o null si no tiene. */
    public static CotizacionResponse toCotizacionResponse(Cotizacion c, Renta renta) {
        Descuento d = c.getDescuento();
        return new CotizacionResponse(
                c.getId(),
                c.getFolio(),
                c.getEstado().name(),
                c.getFechaHoraCreacion(),
                c.getFechaHoraEnvio() == null ? null : c.getFechaHoraEnvio().toLocalDate(),
                c.getFechaRespuesta(),
                c.getValidaHasta(),
                Cotizacion.DIAS_VIGENCIA,
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
                d == null ? TipoDescuento.NINGUNO.name() : d.getTipo().name(),
                d == null ? null : d.getValor(),
                d == null ? null : d.getMotivo(),
                c.getMontoDescuento(),
                c.getIva(),
                c.getTotal(),
                AutorizacionDescuentoResponse.toAutorizacionDescuentoResponse(c.getAutorizacion()),
                renta == null ? null : RentaDeCotizacionResponse.toRentaDeCotizacionResponse(renta));
    }
}