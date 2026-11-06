package mx.montacargasobregon.backend.rentas.captura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Captura manual de una renta (temporal, mientras no se genera desde la cotización).
 * El horario de entrega es opcional: si viene, se confirma en el mismo paso.
 */
public record CrearRentaRequest(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
        @NotBlank(message = "La dirección de la obra es obligatoria")
        @Size(max = 200, message = "La dirección no puede pasar de 200 caracteres") String direccionObra,
        @NotBlank(message = "El contacto de la obra es obligatorio")
        @Size(max = 100, message = "El contacto no puede pasar de 100 caracteres") String contactoObra,
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos") String telefonoContacto,
        @NotEmpty(message = "Debe elegirse al menos un equipo") List<Long> equipoIds,
        LocalDateTime entregaDesde,
        LocalDateTime entregaHasta) {
}