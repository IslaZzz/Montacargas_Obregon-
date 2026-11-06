package mx.montacargasobregon.backend.cartas.dto;

import jakarta.validation.constraints.Size;

/**
 * Lo que se envía al cliente junto con la carta. El destinatario no se elige:
 * siempre es el correo registrado del cliente.
 *
 * @param mensaje el texto del correo; si viene vacío se usa el mensaje sugerido
 */
public record EnviarCartaRequest(
        @Size(max = 2000, message = "El mensaje no puede pasar de 2000 caracteres.") String mensaje) {
}
