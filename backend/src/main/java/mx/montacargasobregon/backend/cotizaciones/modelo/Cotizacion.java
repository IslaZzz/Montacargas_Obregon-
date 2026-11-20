package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import mx.montacargasobregon.backend.usuarios.Usuario;

/**
 * Cotización de renta para un cliente, como en el diagrama de dominio.
 * Traslados (HU-14) y descuento (HU-15) se capturan en la pantalla del precio.
 * La autorización de gerencia (HU-16) ya tiene su lugar, pero todavía no se
 * captura.
 */
@Entity
@Table(name = "cotizaciones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cotizacion {

    // Las fechas se toman con la hora de Sonora, aunque el servidor esté en otra
    // zona
    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Hermosillo");
    // La cotización vale 7 días desde que se crea (HU-17)
    public static final int DIAS_VIGENCIA = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ej. COT-0001
    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    // STRING guarda el nombre del valor; con ORDINAL, reordenar el enum corrompe
    // los datos
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

    // Opcional (HU-15), en su propia tabla: null si no hay descuento (0..1).
    // Se guarda y se borra junto con la cotización. orphanRemoval borra el
    // descuento
    // anterior cuando se cambia o se quita, para que no queden registros sueltos
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "descuento_id")
    private Descuento descuento;

    // Solicitud de autorización del descuento, null si no hay descuento.
    // orphanRemoval borra la solicitud anterior cuando se cambia el descuento
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true)
    @JoinColumn(name = "autorizacion_id")
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

    // Se guardan para que la lista, el detalle y el PDF muestren siempre el mismo
    // precio.
    // subtotal = equipos + traslados, antes del descuento. El IVA se calcula sobre
    // el subtotal con descuento
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

    // True cuando ya se capturó el precio (hoy, al guardar el traslado, HU-14). Con
    // él, la cotización ya puede generar su PDF
    @Column(name = "precio_definido", nullable = false)
    private boolean precioDefinido = false;

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

    /**
     * Guarda el costo de traslado y recalcula el precio (HU-14).
     * Quien llama revisa antes que la cotización siga en Borrador.
     * Con esto el precio queda definido: la cotización ya puede generar su PDF.
     */
    public void registrarTraslado(BigDecimal entrega, BigDecimal recoleccion) {
        this.trasladoEntrega = entrega;
        this.trasladoRecoleccion = recoleccion;
        recalcularPrecio();
        this.precioDefinido = true;
    }

    /**
     * Aplica el descuento, o lo quita si llega null, y recalcula el precio (HU-15).
     * Quien llama revisa antes que la cotización siga en Borrador y que el
     * descuento sea válido.
     * No cambia la tarifa ni el importe de cada equipo.
     */
    public void aplicarDescuento(Descuento nuevoDescuento) {
        this.descuento = nuevoDescuento;
        this.autorizacion = null; // Un descuento nuevo necesita su propia autorización
        recalcularPrecio();
    }

    /** Con descuento, la cotización espera la respuesta de gerencia. */
    public void enviarAAutorizacion(Usuario solicitante) {
        this.autorizacion = new AutorizacionDescuento(solicitante, LocalDateTime.now(ZONA_HORARIA));
        this.estado = EstadoCotizacion.EN_AUTORIZACION;
    }

    /** Gerencia autoriza: vuelve a Borrador, lista para sacar el PDF. */
     public void autorizarDescuento(Usuario gerente, String comentario) {
        autorizacion.autorizar(gerente, comentario, LocalDateTime.now(ZONA_HORARIA));
        this.estado = EstadoCotizacion.BORRADOR;
    }

    /** Gerencia rechaza con un comentario: la cotización queda Rechazada. */
    public void rechazarDescuento(Usuario gerente, String comentario) {
        autorizacion.rechazar(gerente, comentario, LocalDateTime.now(ZONA_HORARIA));
        this.estado = EstadoCotizacion.RECHAZADA;
    }

    /**
     * Pesos que se descuentan sobre el subtotal (equipos + traslados); 0 si no hay
     * descuento.
     * Porcentaje: subtotal × valor / 100, redondeado a centavos. Monto: el valor
     * tal cual.
     */
    public BigDecimal getMontoDescuento() {
        if (descuento == null) {
            return BigDecimal.ZERO;
        }
        return switch (descuento.getTipo()) {
            case PORCENTAJE -> subtotal.multiply(descuento.getValor())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            case MONTO -> descuento.getValor();
            case NINGUNO -> BigDecimal.ZERO;
        };
    }

    /**
     * Cambia el estado y guarda cuándo. Quien llama revisa antes que el cambio se
     * permita.
     */
    public void cambiarEstado(EstadoCotizacion nuevoEstado, LocalDateTime cuando) {
        this.estado = nuevoEstado;
        if (nuevoEstado == EstadoCotizacion.ENVIADA) {
            this.fechaHoraEnvio = cuando;
        } else {
            this.fechaRespuesta = cuando.toLocalDate();
        }
    }

    /**
     * Días de renta, contando el de entrega y el de recolección: del 1 al 5 son 5
     * días (HU-13).
     */
    public long getDias() {
        return ChronoUnit.DAYS.between(fechaHoraEntrega.toLocalDate(), fechaHoraRecoleccion.toLocalDate()) + 1;
    }

    /**
     * subtotal = equipos + traslados (antes del descuento).
     * IVA = (subtotal − descuento) × 16 %; total = subtotal − descuento + IVA.
     */
    private void recalcularPrecio() {
        List<BigDecimal> conceptos = new ArrayList<>(
                equipos.stream().map(EquipoCotizacion::getImporte).toList());
        conceptos.add(trasladoEntrega);
        conceptos.add(trasladoRecoleccion);

        this.subtotal = CalculoPrecio.calcularSubtotal(conceptos);
        BigDecimal conDescuento = subtotal.subtract(getMontoDescuento());
        this.iva = CalculoPrecio.calcularIva(conDescuento);
        this.total = CalculoPrecio.calcularTotal(conDescuento, iva);
    }

    /** Último día en que el cliente puede aceptarla: la fecha de creación + 7 días (HU-17). */
    public LocalDate getValidaHasta() {
        return fechaHoraCreacion.toLocalDate().plusDays(DIAS_VIGENCIA);
    }

    /** True si ya pasó el último día de vigencia. */
    public boolean vigenciaTerminada(LocalDate hoy) {
        return hoy.isAfter(getValidaHasta());
    }

    /** Pasó la vigencia sin respuesta del cliente (HU-17). Quien llama revisa que esté Enviada. */
    public void vencer() {
        this.estado = EstadoCotizacion.VENCIDA;
    }
}
