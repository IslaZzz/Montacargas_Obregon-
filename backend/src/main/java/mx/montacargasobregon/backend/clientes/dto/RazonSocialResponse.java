package mx.montacargasobregon.backend.clientes.dto;

import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.comun.dto.DireccionResponse;

/**
 * Razón social DTO que el backend devuelve al front
 **/
public record RazonSocialResponse(Long id, String nombre, String rfc, DireccionResponse direccionFiscal) {

    public static RazonSocialResponse toRazonSocialResponse(RazonSocial r) {
        return new RazonSocialResponse(
                r.getId(), r.getNombre(), r.getRfc(),
                DireccionResponse.toDireccionResponse(r.getDireccionFiscal()));
    }
}
