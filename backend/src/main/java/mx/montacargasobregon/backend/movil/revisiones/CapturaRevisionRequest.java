package mx.montacargasobregon.backend.movil.revisiones;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoInspeccion;

/**
 * Lo que llenó el mecánico en la revisión: un valor por campo de la categoría.
 * Las fotos no vienen aquí; se suben antes, una por campo FOTO.
 */
public record CapturaRevisionRequest(
        @NotNull(message = "Faltan los campos de la revisión")
        List<@Valid ValorRequest> valores,

        @Size(max = 500, message = "Las notas admiten máximo 500 caracteres")
        String notas) {

    /**
     * El valor de un campo. En los de inspección va el estado (y el detalle si no está bien);
     * en los demás, el valor como texto. Lo que es obligatorio lo revisa el servicio.
     */
    public record ValorRequest(
            @NotNull Long campoId,
            @Size(max = 255, message = "El valor admite máximo 255 caracteres") String valor,
            EstadoInspeccion estado,
            @Size(max = 255, message = "El detalle admite máximo 255 caracteres") String detalle) {
    }
}