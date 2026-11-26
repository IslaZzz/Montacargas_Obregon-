package mx.montacargasobregon.backend.traslados.dto;

/** Si un traslado ya se puede marcar realizado desde la oficina y, si no, por qué. */
public record RealizableResponse(boolean realizable, String motivo) {
}