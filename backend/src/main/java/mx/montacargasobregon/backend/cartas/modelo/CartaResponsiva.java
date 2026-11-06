package mx.montacargasobregon.backend.cartas.modelo;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Carta responsiva de una renta (OHU-04). Si una renta no tiene registro, su carta está SIN_GENERAR.
 * Solo se guarda la versión vigente: al regenerarla se reemplaza el PDF y sube el número de versión.
 * El PDF se guarda tal como se generó, para que lo que se envía y se firma no cambie
 * aunque después cambien los datos de la renta.
 */
@Entity
@Table(name = "cartas_responsivas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartaResponsiva {

    // Misma zona que cotizaciones: la empresa trabaja con la hora de Sonora
    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Hermosillo");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Una sola carta por renta
    @OneToOne(optional = false)
    @JoinColumn(name = "renta_id", nullable = false, unique = true)
    private Renta renta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCarta estado;

    // Fecha de la versión vigente
    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    // 1 al generarla; sube cada vez que se regenera
    @Column(name = "version", nullable = false)
    private int numeroVersion;

    // Los datos con que se armó el PDF, un renglón "campo<TAB>valor" por dato.
    // Sirven para avisar si la renta cambió después de generar la carta.
    // Vacíos en las cartas de los datos de prueba, que no tienen PDF.
    @Lob
    @Column(name = "datos", columnDefinition = "TEXT")
    private String datos;

    // El archivo pesa pocos KB
    @Lob
    @Column(name = "pdf", columnDefinition = "LONGBLOB")
    private byte[] pdf;

    /** Crea la carta recién generada, versión 1. */
    public CartaResponsiva(Renta renta, String datos, byte[] pdf) {
        this.renta = renta;
        this.numeroVersion = 1;
        guardarVersion(datos, pdf);
    }

    /** Reemplaza el PDF por una versión nueva; la anterior deja de estar vigente y hay que volver a enviarla. */
    public void regenerar(String datos, byte[] pdf) {
        this.numeroVersion++;
        guardarVersion(datos, pdf);
    }

    private void guardarVersion(String datos, byte[] pdf) {
        this.datos = datos;
        this.pdf = pdf;
        this.estado = EstadoCarta.GENERADA;
        this.fechaGeneracion = LocalDateTime.now(ZONA_HORARIA);
    }

    public void cambiarEstado(EstadoCarta nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public boolean tieneArchivo() {
        return pdf != null && pdf.length > 0;
    }

    /** Ej. Carta_responsiva_R-2070_v2.pdf */
    public String nombreArchivo() {
        return "Carta_responsiva_%s_v%d.pdf".formatted(renta.getFolio(), Math.max(numeroVersion, 1));
    }
}
