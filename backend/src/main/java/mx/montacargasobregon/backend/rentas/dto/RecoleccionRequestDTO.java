package mx.montacargasobregon.backend.rentas.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

public record RecoleccionRequestDTO(
    @NotNull LocalDateTime desde,
    @NotNull LocalDateTime hasta
) {}
