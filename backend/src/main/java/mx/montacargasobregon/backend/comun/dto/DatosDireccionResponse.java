package mx.montacargasobregon.backend.comun.dto;

import mx.montacargasobregon.backend.comun.DatosDireccion;

/** Dirección del lugar de entrega que el backend devuelve al front. */
public record DatosDireccionResponse(Long id, String calle, String numero, String colonia, String ciudad,
        String estado, String codigoPostal) {

    public static DatosDireccionResponse toDatosDireccionResponse(DatosDireccion d) {
        return new DatosDireccionResponse(d.getId(), d.getCalle(), d.getNumero(), d.getColonia(), d.getCiudad(),
                d.getEstado(), d.getCodigoPostal());
    }
}
