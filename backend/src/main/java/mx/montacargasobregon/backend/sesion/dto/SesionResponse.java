package mx.montacargasobregon.backend.sesion.dto;

import mx.montacargasobregon.backend.usuarios.Usuario;

/** El usuario que tiene la sesión abierta: lo que se muestra en el encabezado y su menú (HU-28). */
public record SesionResponse(String nombre, String correo, String rol) {

    public static SesionResponse toSesionResponse(Usuario u) {
        return new SesionResponse(u.getNombres() + " " + u.getApellidoPaterno(), u.getCorreo(), u.getTipo().name());
    }
}
