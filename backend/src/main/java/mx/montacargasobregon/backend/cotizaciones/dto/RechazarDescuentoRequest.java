package mx.montacargasobregon.backend.cotizaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo que escribe gerencia al rechazar un descuento. */
public record RechazarDescuentoRequest(
        @NotBlank(message = "Escribe el motivo del rechazo")
        @Size(max = 500, message = "El motivo no puede pasar de 500 caracteres") String comentario) {
}