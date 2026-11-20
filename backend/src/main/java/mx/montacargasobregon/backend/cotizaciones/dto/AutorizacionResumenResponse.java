package mx.montacargasobregon.backend.cotizaciones.dto;

import java.math.BigDecimal;

import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.cotizaciones.modelo.Descuento;

/** Una fila de la lista «Autorizaciones» de gerencia. */
public record AutorizacionResumenResponse(
        Long cotizacionId,
        String folio,
        String cliente,
        String empresa,
        String descuentoTipo,
        BigDecimal descuentoValor,
        BigDecimal descuentoMonto, // Pesos que se descuentan
        String descuentoMotivo,
        BigDecimal total, // Total con IVA y con descuento
        AutorizacionDescuentoResponse autorizacion) {

    public static AutorizacionResumenResponse toAutorizacionResumenResponse(Cotizacion c) {
        Cliente cliente = c.getCliente();
        Descuento d = c.getDescuento();
        String nombre = cliente.getNombre() + " " + cliente.getApellidoPaterno();
        return new AutorizacionResumenResponse(
                c.getId(),
                c.getFolio(),
                cliente.getApellidoMaterno() == null ? nombre : nombre + " " + cliente.getApellidoMaterno(),
                cliente.getEmpresa(),
                d.getTipo().name(),
                d.getValor(),
                c.getMontoDescuento(),
                d.getMotivo(),
                c.getTotal(),
                AutorizacionDescuentoResponse.toAutorizacionDescuentoResponse(c.getAutorizacion()));
    }
}