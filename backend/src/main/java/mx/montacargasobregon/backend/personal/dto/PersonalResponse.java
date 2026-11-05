package mx.montacargasobregon.backend.personal.dto;

import mx.montacargasobregon.backend.personal.modelo.Personal;

/** Personal DTO de respuesta que el backend devuelve al front. */
public record PersonalResponse(Long id, String codigo, String nombres,
                               String apellidoPaterno, String apellidoMaterno, String tipo, boolean activo) {

    public static PersonalResponse toPersonalResponse(Personal p) {
        return new PersonalResponse(
                p.getId(), p.getCodigo(), p.getNombres(),
                p.getApellidoPaterno(), p.getApellidoMaterno(),
                p.getTipo().name(), p.isActivo());
    }
}