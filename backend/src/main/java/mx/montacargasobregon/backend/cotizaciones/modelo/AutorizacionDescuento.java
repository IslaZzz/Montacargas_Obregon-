package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.usuarios.Usuario;

/**
 * Solicitud de autorización de un descuento y la respuesta de gerencia (HU-16).
 * Se crea cuando el auxiliar aplica un descuento; gerencia la autoriza o la
 * rechaza.
 */
@Entity
@Table(name = "autorizaciones_descuento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutorizacionDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoAutorizacion resultado;

    // Quién pidió la autorización (el auxiliar) y cuándo
    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitada_por_id", nullable = false)
    private Usuario solicitadaPor;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    // Quién respondió (gerencia) y cuándo; vacíos mientras esté pendiente
    @ManyToOne
    @JoinColumn(name = "respondida_por_id")
    private Usuario respondidaPor;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    // Obligatorio al rechazar; vacío al autorizar
    @Column(length = 500)
    private String comentario;

    public AutorizacionDescuento(Usuario solicitadaPor, LocalDateTime fechaSolicitud) {
        this.solicitadaPor = solicitadaPor;
        this.fechaSolicitud = fechaSolicitud;
        this.resultado = ResultadoAutorizacion.PENDIENTE;
    }

    public void autorizar(Usuario gerente, String comentario, LocalDateTime cuando) {
        this.resultado = ResultadoAutorizacion.AUTORIZADA;
        this.respondidaPor = gerente;
        this.fechaRespuesta = cuando;
        this.comentario = comentario;
    }

    public void rechazar(Usuario gerente, String comentario, LocalDateTime cuando) {
        this.resultado = ResultadoAutorizacion.RECHAZADA;
        this.respondidaPor = gerente;
        this.fechaRespuesta = cuando;
        this.comentario = comentario;
    }
}