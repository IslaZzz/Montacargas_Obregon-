package mx.montacargasobregon.backend.movil.fotos;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.personal.modelo.Personal;

/**
 * Foto tomada desde la app móvil. Sirve para cualquier historia: se liga por origen y referenciaId
 * (por ejemplo REVISION 15) y se distingue por etiqueta ("Frente", "Tablero y horómetro").
 * Hay a lo más una foto por etiqueta; volver a tomarla reemplaza la anterior.
 */
@Entity
@Table(name = "fotos", indexes = @Index(name = "idx_fotos_referencia", columnList = "origen, referencia_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenFoto origen;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false, length = 50)
    private String etiqueta;

    @Column(name = "tipo_contenido", nullable = false, length = 30)
    private String tipoContenido;

    // MEDIUMBLOB admite hasta 16 MB; FotoService limita cada foto a 5 MB
    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] contenido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subida_por_id", nullable = false)
    private Personal subidaPor;

    @Column(name = "subida_en", nullable = false)
    private LocalDateTime subidaEn;

    public Foto(OrigenFoto origen, Long referenciaId, String etiqueta, String tipoContenido,
                byte[] contenido, Personal subidaPor, LocalDateTime subidaEn) {
        this.origen = origen;
        this.referenciaId = referenciaId;
        this.etiqueta = etiqueta;
        this.tipoContenido = tipoContenido;
        this.contenido = contenido;
        this.subidaPor = subidaPor;
        this.subidaEn = subidaEn;
    }
}