package mx.montacargasobregon.backend.comun.dto;

import mx.montacargasobregon.backend.comun.Direccion;

/**
 * Domicilio para mostrar en la app móvil. texto siempre viene; las partes vienen cuando se tienen.
 *   Obra de una renta:  DomicilioResponse.deDireccion(renta.getCotizacion().getDireccionObra())
 *   Solo texto:         DomicilioResponse.deTexto(renta.getDireccionObra())
 */
public record DomicilioResponse(String texto, String calle, String numero, String colonia,
                                String codigoPostal, String ciudad, String estado) {

    public static DomicilioResponse deTexto(String texto) {
        return new DomicilioResponse(texto, null, null, null, null, null, null);
    }

    public static DomicilioResponse deDireccion(Direccion d) {
        return new DomicilioResponse(d.enUnRenglon(), d.getCalle(), d.getNumero(), d.getColonia(),
                d.getCodigoPostal(), d.getCiudad(), d.getEstado());
    }
}