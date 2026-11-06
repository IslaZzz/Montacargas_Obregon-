package mx.montacargasobregon.backend.revisiones.modelo;

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
import mx.montacargasobregon.backend.rentas.modelo.EquipoRenta;

/**
 * Revisión de un equipo dentro de una renta, a la salida o al regreso.
 */
@Entity
@Table(name = "revisiones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipo_renta_id", nullable = false)
    private EquipoRenta equipoRenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoRevision tipo;

    // Cuándo tiene que estar lista, el inicio de la entrega o el fin de la recolección
    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    // Vacío mientras está PENDIENTE
    @ManyToOne
    @JoinColumn(name = "mecanico_id")
    private Personal mecanico;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRevision estado;

    // Vacío hasta que se termina
    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private ResultadoRevision resultado;

    @Column(length = 500)
    private String notas;

    public Revision(EquipoRenta equipoRenta, TipoRevision tipo, LocalDateTime fechaProgramada) {
        this.equipoRenta = equipoRenta;
        this.tipo = tipo;
        this.fechaProgramada = fechaProgramada;
        this.estado = EstadoRevision.PENDIENTE;
    }

    /** Se puede asignar o cambiar de mecánico mientras no esté terminada ni cancelada. */
    public boolean puedeAsignarse() {
        return estado == EstadoRevision.PENDIENTE
                || estado == EstadoRevision.ASIGNADO
                || estado == EstadoRevision.EN_PROCESO;
    }

    /**
     * Asigna o cambia el mecánico. Una pendiente pasa a ASIGNADO;
     * una que ya estaba en proceso conserva su estado.
     */
    public void asignar(Personal mecanico) {
        this.mecanico = mecanico;
        this.fechaAsignacion = LocalDateTime.now();
        if (estado == EstadoRevision.PENDIENTE) {
            estado = EstadoRevision.ASIGNADO;
        }
    }
}