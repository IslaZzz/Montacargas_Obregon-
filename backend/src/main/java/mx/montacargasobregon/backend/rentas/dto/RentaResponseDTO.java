package mx.montacargasobregon.backend.rentas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import mx.montacargasobregon.backend.rentas.modelo.EstadoRenta;

public record RentaResponseDTO(
    Long id,
    String folio,
    ClienteRentaDTO cliente,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    LocalDateTime entregaDesde,
    LocalDateTime entregaHasta,
    LocalDateTime recoleccionDesde,
    LocalDateTime recoleccionHasta,
    String direccionObra,
    String contactoObra,
    String telefonoContacto,
    EstadoRenta estado,
    List<EquipoRentaDTO> equipos
) {}
