package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.clientes.modelo.RazonSocial;
import mx.montacargasobregon.backend.comun.DatosContactoSitio;
import mx.montacargasobregon.backend.comun.DatosDireccion;

/**
 * Cotización de renta para un cliente, como en el diagrama de dominio.
 * Traslados (HU-14), descuento (HU-15) y la autorización de gerencia (HU-16) ya tienen su lugar,
 * pero todavía no se capturan: los traslados quedan en 0 y la cotización va sin descuento.
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

    // STRING guarda el nombre del valor; con ORDINAL, reordenar el enum corrompe los datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaHoraCreacion;

    @Column(name = "fecha_recoleccion", nullable = false)
    private LocalDateTime fechaHoraRecoleccion;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaHoraEntrega;

    // Cada cotización tiene su propia dirección de obra y se guarda junto con ella
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, optional = false)
    @JoinColumn(name = "direccion_obra_id", nullable = false)
    private DatosDireccion direccionObra;

    @Embedded
    @AttributeOverride(name = "nombre", column = @Column(name = "contacto_obra", nullable = false, length = 100))
    @AttributeOverride(name = "telefono", column = @Column(name = "telefono_contacto", nullable = false, length = 10))
    private DatosContactoSitio contactoObra;

    // Costo de llevar y de recoger el equipo, capturado a mano (HU-14)
    @Column(name = "traslado_entrega", nullable = false, precision = 12, scale = 2)
    private BigDecimal trasladoEntrega = BigDecimal.ZERO;

    @Column(name = "traslado_recoleccion", nullable = false, precision = 12, scale = 2)
    private BigDecimal trasladoRecoleccion = BigDecimal.ZERO;

    // Opcional (HU-15): sin descuento queda vacío
    @Embedded
    private Descuento descuento;

    // Respuesta de gerencia al descuento (HU-16); vacía mientras no la haya
    @Embedded
    private AutorizacionDescuento autorizacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // La razón social del cliente a la que se factura
    @ManyToOne(optional = false)
    @JoinColumn(name = "razon_social_id", nullable = false)
    private RazonSocial razonSocial;

    // Los equipos se guardan junto con la cotización
    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private List<EquipoCotizacion> equipos = new ArrayList<>();

    // Se guardan al crearla para que la lista, el detalle y el PDF muestren siempre el mismo precio
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // Cuándo pasó a Enviada (HU-18)
    @Column(name = "fecha_envio")
    private LocalDateTime fechaHoraEnvio;

    // Cuándo se marcó como Aceptada o No aceptada (HU-20)
    @Column(name = "fecha_respuesta")
    private LocalDate fechaRespuesta;

    public Cotizacion(String folio, Cliente cliente, RazonSocial razonSocial,
                      LocalDateTime fechaHoraEntrega, LocalDateTime fechaHoraRecoleccion,
                      DatosDireccion direccionObra, DatosContactoSitio contactoObra,
                      BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        this.folio = folio;
        this.cliente = cliente;
        this.razonSocial = razonSocial;
        this.fechaHoraEntrega = fechaHoraEntrega;
        this.fechaHoraRecoleccion = fechaHoraRecoleccion;
        this.direccionObra = direccionObra;
        this.contactoObra = contactoObra;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.estado = EstadoCotizacion.BORRADOR;
        this.fechaHoraCreacion = LocalDateTime.now(ZONA_HORARIA);
    }

    public void agregarEquipo(EquipoCotizacion equipo) {
        equipos.add(equipo);
    }

    /** Cambia el estado y guarda cuándo. Quien llama revisa antes que el cambio se permita. */
    public void cambiarEstado(EstadoCotizacion nuevoEstado, LocalDateTime cuando) {
        this.estado = nuevoEstado;
        if (nuevoEstado == EstadoCotizacion.ENVIADA) {
            this.fechaHoraEnvio = cuando;
        } else {
            this.fechaRespuesta = cuando.toLocalDate();
        }
    }

    /** Días de renta, contando el de entrega y el de recolección: del 1 al 5 son 5 días (HU-13). */
    public long getDias() {
        return ChronoUnit.DAYS.between(fechaHoraEntrega.toLocalDate(), fechaHoraRecoleccion.toLocalDate()) + 1;
    }
}
