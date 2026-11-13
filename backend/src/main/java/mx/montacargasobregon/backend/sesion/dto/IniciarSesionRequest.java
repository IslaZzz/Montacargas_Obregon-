package mx.montacargasobregon.backend.sesion.dto;

import jakarta.validation.constraints.NotBlank;

/** Lo que se escribe en la pantalla «Iniciar sesión» (HU-28). */
public record IniciarSesionRequest(
        @NotBlank(message = "Escribe tu correo") String correo,
        @NotBlank(message = "Escribe tu contraseña") String contrasena) {
}
