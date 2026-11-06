package mx.montacargasobregon.backend.cartas.dto;

import java.time.LocalDateTime;

import mx.montacargasobregon.backend.cartas.modelo.TipoFirma;

/**
 * La firma de la carta (OHU-05).
 *
 * @param fecha         cuándo firmó el cliente
 * @param registradaPor quién la registró en el sistema, con su rol
 * @param registradaEn  cuándo se registró
 * @param tipoArchivo   application/pdf, image/jpeg o image/png
 */
public record FirmaCarta(
        TipoFirma tipo,
        LocalDateTime fecha,
        String firmante,
        String registradaPor,
        String rolRegistro,
        LocalDateTime registradaEn,
        String nombreArchivo,
        String tipoArchivo) {
}
