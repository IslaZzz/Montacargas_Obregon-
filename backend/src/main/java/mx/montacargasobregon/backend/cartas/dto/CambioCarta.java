package mx.montacargasobregon.backend.cartas.dto;

/** Un dato de la renta que cambió después de generar la carta. Ej. Fecha final: 08/11/2026 → 11/11/2026 */
public record CambioCarta(String campo, String antes, String ahora) {
}
