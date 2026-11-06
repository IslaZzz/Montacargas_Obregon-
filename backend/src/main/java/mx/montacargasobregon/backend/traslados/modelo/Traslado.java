package mx.montacargasobregon.backend.traslados.modelo;

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
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Movimiento físico de maquinaria para entrega o recolección de una renta.
 * Relaciona la renta con el operador de traslado responsable y el bloque de horario programado.
 */
@Entity
@Table(name = "traslados")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Traslado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Folio operativo, p. ej. TR-0086
    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "renta_id", nullable = false)
    private Renta renta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTraslado tipo;

    // Ventana programada del traslado
    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    // Operador de traslado asignado (vacío mientras no se asigne)
    @ManyToOne
    @JoinColumn(name = "responsable_id")
    private Personal responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTraslado estado;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_inicio_real")
    private LocalDateTime fechaInicioReal;

    @Column(name = "fecha_fin_real")
    private LocalDateTime fechaFinReal;

    @Column(length = 500)
    private String notas;

    public Traslado(String folio, Renta renta, TipoTraslado tipo,
                    LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin) {
        this.folio = folio;
        this.renta = renta;
        this.tipo = tipo;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.estado = EstadoTraslado.PROGRAMADO;
    }

    /**
     * Indica si el traslado ya inició en campo (o ya fue completado).
     * En este caso no se permite cambiar de operador desde la oficina.
     */
    public boolean isYaInicio() {
        return estado == EstadoTraslado.EN_CURSO
                || estado == EstadoTraslado.COMPLETADO
                || fechaInicioReal != null;
    }

    /** Se puede asignar o cambiar de responsable mientras no haya iniciado ni esté cancelado. */
    public boolean puedeAsignarse() {
        return !isYaInicio() && estado != EstadoTraslado.CANCELADO;
    }

    /** Asigna o cambia el chofer/operador responsable. */
    public void asignarResponsable(Personal nuevoResponsable) {
        this.responsable = nuevoResponsable;
        this.fechaAsignacion = LocalDateTime.now();
    }

    /** Desasigna el chofer/operador. */
    public void desasignar() {
        this.responsable = null;
        this.fechaAsignacion = null;
    }

    /** Inicia el traslado en campo. */
    public void iniciar() {
        this.estado = EstadoTraslado.EN_CURSO;
        this.fechaInicioReal = LocalDateTime.now();
    }

    /** Completa el traslado en campo. */
    public void completar() {
        this.estado = EstadoTraslado.COMPLETADO;
        this.fechaFinReal = LocalDateTime.now();
    }

    /** Cancela el traslado. */
    public void cancelar() {
        this.estado = EstadoTraslado.CANCELADO;
    }
}

