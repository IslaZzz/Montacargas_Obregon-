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

    // Cuándo y a qué correo se envió la versión vigente; vacíos si no se ha enviado
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "correo_envio", length = 150)
    private String correoEnvio;

    // ===== Firma (OHU-05); vacíos mientras no se firma =====
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_firma", length = 20)
    private TipoFirma tipoFirma;

    // Cuándo firmó el cliente, según quien registra la firma
    @Column(name = "fecha_firma")
    private LocalDateTime fechaFirma;

    @Column(name = "firmante", length = 150)
    private String firmante;

    // Quién registró la firma en el sistema y cuándo
    @Column(name = "firma_registrada_por", length = 150)
    private String firmaRegistradaPor;

    @Column(name = "firma_registrada_rol", length = 60)
    private String firmaRegistradaRol;

    @Column(name = "firma_registrada_en")
    private LocalDateTime firmaRegistradaEn;

    // La carta firmada: el PDF que regresó el cliente o la foto/escaneo de la carta impresa (máx. 10 MB)
    @Lob
    @Column(name = "archivo_firmado", columnDefinition = "LONGBLOB")
    private byte[] archivoFirmado;

    // application/pdf, image/jpeg o image/png
    @Column(name = "tipo_archivo_firmado", length = 40)
    private String tipoArchivoFirmado;

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
        // La versión nueva todavía no se envía
        this.fechaEnvio = null;
        this.correoEnvio = null;
    }

    /** La versión vigente ya se envió al cliente: pasa a Enviada. */
    public void registrarEnvio(String correo) {
        this.estado = EstadoCarta.ENVIADA;
        this.fechaEnvio = LocalDateTime.now(ZONA_HORARIA);
        this.correoEnvio = correo;
    }

    /**
     * El cliente firmó la carta: pasa a Firmada. Desde ese momento no se regenera, no se envía
     * y la firma no se cambia.
     */
    public void registrarFirma(TipoFirma tipo, LocalDateTime fecha, String firmante,
                               String registradaPor, String rol, byte[] archivo, String tipoArchivo) {
        this.estado = EstadoCarta.FIRMADA;
        this.tipoFirma = tipo;
        this.fechaFirma = fecha;
        this.firmante = firmante;
        this.firmaRegistradaPor = registradaPor;
        this.firmaRegistradaRol = rol;
        this.firmaRegistradaEn = LocalDateTime.now(ZONA_HORARIA);
        this.archivoFirmado = archivo;
        this.tipoArchivoFirmado = tipoArchivo;
    }

    public boolean tieneArchivoFirmado() {
        return archivoFirmado != null && archivoFirmado.length > 0;
    }

    /** Ej. Carta_firmada_R-2070.pdf o Carta_firmada_R-2070.jpg */
    public String nombreArchivoFirmado() {
        String extension = switch (tipoArchivoFirmado == null ? "" : tipoArchivoFirmado) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> "pdf";
        };
        return "Carta_firmada_%s.%s".formatted(renta.getFolio(), extension);
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
