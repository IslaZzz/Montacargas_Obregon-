package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Una vez que el auxiliar corrigió una cotización que gerencia rechazó (HU-32).
 * Copia el rechazo, porque la solicitud de autorización se reemplaza al corregir,
 * y así el detalle conserva el historial.
 */
@Entity
@Table(name = "correcciones_cotizacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorreccionCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El rechazo de gerencia: quién, cuándo y por qué
    @ManyToOne(optional = false)
    @JoinColumn(name = "rechazada_por_id", nullable = false)
    private Usuario rechazadaPor;

    @Column(name = "fecha_rechazo", nullable = false)
    private LocalDateTime fechaRechazo;

    @Column(name = "comentario_rechazo", length = 500)
    private String comentarioRechazo;

    // Quién la corrigió y cuándo
    @ManyToOne(optional = false)
    @JoinColumn(name = "corregida_por_id", nullable = false)
    private Usuario corregidaPor;

    @Column(name = "fecha_correccion", nullable = false)
    private LocalDateTime fechaCorreccion;

    // True si siguió con descuento y volvió a autorización; false si quedó en Borrador sin descuento
    @Column(nullable = false)
    private boolean reenviada;

    public CorreccionCotizacion(AutorizacionDescuento rechazo, Usuario corregidaPor, LocalDateTime fechaCorreccion,
            boolean reenviada) {
        this.rechazadaPor = rechazo.getRespondidaPor();
        this.fechaRechazo = rechazo.getFechaRespuesta();
        this.comentarioRechazo = rechazo.getComentario();
        this.corregidaPor = corregidaPor;
        this.fechaCorreccion = fechaCorreccion;
        this.reenviada = reenviada;
    }
}
