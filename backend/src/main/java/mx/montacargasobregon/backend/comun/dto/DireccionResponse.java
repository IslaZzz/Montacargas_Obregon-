package mx.montacargasobregon.backend.comun.dto;

import mx.montacargasobregon.backend.comun.Direccion;

/**
 * DTO que representa una dirección que el backend devuelve al front
 */
public record DireccionResponse(Long id, String calle, String numero, String colonia, String ciudad,
        String codigoPostal) {

    public static DireccionResponse toDireccionResponse(Direccion d) {
        return new DireccionResponse(
                d.getId(), d.getCalle(), d.getNumero(), d.getColonia(), d.getCiudad(), d.getCodigoPostal());
    }
}
