package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.usuarios.Usuario;

/**
 * Respuesta de gerencia a una cotización con descuento (HU-16). No tiene tabla propia: va en la de cotizaciones.
 * Mientras nadie responda, la cotización no la tiene.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutorizacionDescuento {

    @ManyToOne
    @JoinColumn(name = "autorizacion_usuario_id")
    private Usuario autorizadaPor;

    @Column(name = "autorizacion_fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    // Al rechazar, gerencia explica por qué
    @Column(name = "autorizacion_comentario", length = 500)
    private String comentario;

    public AutorizacionDescuento(Usuario autorizadaPor, LocalDateTime fechaRespuesta, String comentario) {
        this.autorizadaPor = autorizadaPor;
        this.fechaRespuesta = fechaRespuesta;
        this.comentario = comentario;
    }
}
