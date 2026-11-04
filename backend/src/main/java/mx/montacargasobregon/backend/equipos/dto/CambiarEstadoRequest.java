package mx.montacargasobregon.backend.equipos.dto;

import jakarta.validation.constraints.NotNull;
import mx.montacargasobregon.backend.equipos.modelo.EstadoEquipo;

/** Nuevo estado que se le asigna a un equipo. */
public record CambiarEstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoEquipo estado) {
}