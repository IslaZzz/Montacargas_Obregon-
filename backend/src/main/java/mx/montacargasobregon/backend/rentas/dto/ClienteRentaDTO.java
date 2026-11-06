package mx.montacargasobregon.backend.rentas.dto;

public record ClienteRentaDTO(
    Long id,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    String empresa
) {}
