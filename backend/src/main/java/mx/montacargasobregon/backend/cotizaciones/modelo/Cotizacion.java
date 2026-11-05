package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;

/**
 * Cotización de renta para un cliente.
 * Base para consultarla y cambiar su estado (HU-18 y HU-20). Crearla, con su folio, días, importes,
 * subtotal, IVA y total, es de HU-12, HU-13 y HU-33: quien la implemente puede ajustar el constructor.
 */
@Entity
@Table(name = "cotizaciones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cotizacion {

    // Las fechas se toman con la hora de Sonora, aunque el servidor esté en otra zona
    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Hermosillo");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ej. COT-0001
    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // La razón social del cliente a la que se factura
    @ManyToOne(optional = false)
    @JoinColumn(name = "razon_social_id", nullable = false)
    private RazonSocial razonSocial;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_recoleccion", nullable = false)
    private LocalDateTime fechaRecoleccion;

    @Column(name = "direccion_obra", nullable = false, length = 200)
    private String direccionObra;

    @Column(name = "contacto_obra", nullable = false, length = 100)
    private String contactoObra;

    @Column(name = "telefono_contacto", nullable = false, length = 10)
    private String telefonoContacto;

    // Los equipos se guardan junto con la cotización
    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private List<PartidaCotizacion> partidas = new ArrayList<>();

    // Se guardan al crearla para que la lista, el detalle y el PDF muestren siempre el mismo precio
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // STRING guarda el nombre del valor; con ORDINAL, reordenar el enum corrompe los datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // Cuándo pasó a Enviada (HU-18)
    @Column(name = "fecha_envio")
    private LocalDate fechaEnvio;

    // Cuándo se marcó como Aceptada o No aceptada (HU-20)
    @Column(name = "fecha_respuesta")
    private LocalDate fechaRespuesta;

    public Cotizacion(String folio, Cliente cliente, RazonSocial razonSocial,
                      LocalDateTime fechaEntrega, LocalDateTime fechaRecoleccion,
                      String direccionObra, String contactoObra, String telefonoContacto,
                      BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        this.folio = folio;
        this.cliente = cliente;
        this.razonSocial = razonSocial;
        this.fechaEntrega = fechaEntrega;
        this.fechaRecoleccion = fechaRecoleccion;
        this.direccionObra = direccionObra;
        this.contactoObra = contactoObra;
        this.telefonoContacto = telefonoContacto;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.estado = EstadoCotizacion.BORRADOR;
        this.fechaCreacion = LocalDateTime.now(ZONA_HORARIA);
    }

    public void agregarPartida(PartidaCotizacion partida) {
        partidas.add(partida);
    }

    /** Días de renta, contando el de entrega y el de recolección: del 1 al 5 son 5 días (HU-13). */
    public long getDias() {
        return ChronoUnit.DAYS.between(fechaEntrega.toLocalDate(), fechaRecoleccion.toLocalDate()) + 1;
    }
}
