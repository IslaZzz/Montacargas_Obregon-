package mx.montacargasobregon.backend.cotizaciones.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearCotizacionRequest(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @NotNull(message = "La razón social es obligatoria") Long razonSocialId,
        @NotNull(message = "La fecha y hora de entrega es obligatoria") LocalDateTime fechaEntrega,
        @NotNull(message = "La fecha y hora de recolección es obligatoria") LocalDateTime fechaRecoleccion,
        @NotBlank(message = "Escribe la dirección de la obra")
        @Size(max = 200, message = "La dirección admite máximo 200 caracteres") String direccionObra,
        @NotBlank(message = "Escribe quién recibe en la obra")
        @Size(max = 100, message = "El contacto admite máximo 100 caracteres") String contactoObra,
        @NotBlank(message = "Escribe el teléfono del contacto")
        @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos") String telefonoContacto,
        @NotEmpty(message = "Agrega al menos un equipo") List<@NotNull Long> equipoIds) {
}
