package mx.montacargasobregon.backend.rentas.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.clientes.modelo.Cliente;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;

/**
 * Renta de uno o varios equipos a un cliente.
 * Nace al aceptar la cotización (HU-37); en Delivery S3 nacerá al registrar el anticipo del contrato.
 * Los datos de la obra se copian para que, si cambian durante la renta,
 * no se altere el documento que el cliente aceptó.
 */
@Entity
@Table(name = "rentas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Renta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ej. R-0001
    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Cotización de la que salió; cada cotización genera una sola renta
    @OneToOne(optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false, unique = true)
    private Cotizacion cotizacion;

    // Periodo contratado: lo que se cobra
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    // Horarios confirmados con el cliente; vacíos hasta que se confirman.
    // Son rangos porque los traslados se programan por ventana y se revisan empalmes.
    @Column(name = "entrega_desde")
    private LocalDateTime entregaDesde;

    @Column(name = "entrega_hasta")
    private LocalDateTime entregaHasta;

    @Column(name = "recoleccion_desde")
    private LocalDateTime recoleccionDesde;

    @Column(name = "recoleccion_hasta")
    private LocalDateTime recoleccionHasta;

    // La dirección de la obra de la cotización, en un renglón. Cabe aunque todos sus campos lleguen al máximo
    @Column(name = "direccion_obra", nullable = false, length = 400)
    private String direccionObra;

    @Column(name = "contacto_obra", nullable = false, length = 100)
    private String contactoObra;

    @Column(name = "telefono_contacto", nullable = false, length = 10)
    private String telefonoContacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRenta estado;

    // Los equipos son parte de la renta: se guardan y se quitan con ella
    @OneToMany(mappedBy = "renta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EquipoRenta> equipos = new ArrayList<>();

    @OneToMany(mappedBy = "renta")
    private List<mx.montacargasobregon.backend.traslados.modelo.Traslado> traslados = new ArrayList<>();

    /**
     * Crea la renta a partir de la cotización aceptada: copia el cliente, el periodo, la obra, su contacto
     * y los equipos, que empiezan RESERVADO. Los horarios de entrega y recolección quedan por confirmar.
     */
    public Renta(String folio, Cotizacion cotizacion) {
        this.folio = folio;
        this.cotizacion = cotizacion;
        this.cliente = cotizacion.getCliente();
        this.fechaInicio = cotizacion.getFechaHoraEntrega().toLocalDate();
        this.fechaFin = cotizacion.getFechaHoraRecoleccion().toLocalDate();
        this.direccionObra = cotizacion.getDireccionObra().enUnRenglon();
        this.contactoObra = cotizacion.getContactoObra().getNombre();
        this.telefonoContacto = cotizacion.getContactoObra().getTelefono();
        this.estado = EstadoRenta.PROGRAMADA;
        cotizacion.getEquipos().forEach(e -> agregarEquipo(e.getEquipo()));
    }

    /** Agrega un equipo a la renta; empieza RESERVADO. */
    public EquipoRenta agregarEquipo(Equipo equipo) {
        EquipoRenta equipoRenta = new EquipoRenta(this, equipo);
        equipos.add(equipoRenta);
        return equipoRenta;
    }

    /** Guarda el horario de entrega acordado. Quien llama revisa que desde sea antes que hasta. */
    public void confirmarEntrega(LocalDateTime desde, LocalDateTime hasta) {
        this.entregaDesde = desde;
        this.entregaHasta = hasta;
    }

    /** Guarda el horario de recolección acordado. Quien llama revisa que desde sea antes que hasta. */
    public void confirmarRecoleccion(LocalDateTime desde, LocalDateTime hasta) {
        this.recoleccionDesde = desde;
        this.recoleccionHasta = hasta;
    }

    public void cambiarEstado(EstadoRenta nuevoEstado) {
        this.estado = nuevoEstado;
    }
}