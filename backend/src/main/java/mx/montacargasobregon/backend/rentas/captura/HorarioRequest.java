package mx.montacargasobregon.backend.rentas.captura;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/** Ventana acordada con el cliente para entregar o recoger. */
public record HorarioRequest(
        @NotNull(message = "Indica cuándo empieza el horario") LocalDateTime desde,
        @NotNull(message = "Indica cuándo termina el horario") LocalDateTime hasta) {
}