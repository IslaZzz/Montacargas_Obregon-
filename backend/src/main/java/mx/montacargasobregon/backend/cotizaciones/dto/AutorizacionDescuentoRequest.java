package mx.montacargasobregon.backend.cotizaciones.dto;

import jakarta.validation.constraints.Size;

/** Lo que puede escribir gerencia al autorizar un descuento. */
public record AutorizacionDescuentoRequest(
        @Size(max = 500, message = "El comentario no puede pasar de 500 caracteres") String comentario) {
}