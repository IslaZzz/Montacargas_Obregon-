package mx.montacargasobregon.backend.mantenimientos.modelo;

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
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.personal.modelo.Personal;

/**
 * Reparación o servicio sobre un equipo.
 * La falla, quién la reportó y cuándo viven aquí porque un preventivo no viene
 * de ninguna revisión. Cuando exista Revision, solo se agrega la relación opcional.
 */
@Entity
@Table(name = "mantenimientos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMantenimiento tipo;

    @Column(name = "descripcion_falla", nullable = false, length = 255)
    private String descripcionFalla;

    // Quién detectó la falla; un preventivo puede no tenerlo
    @ManyToOne
    @JoinColumn(name = "reportado_por_id")
    private Personal reportadoPor;

    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    // Cuándo tiene que estar listo: la salida programada del equipo
    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

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
    private EstadoMantenimiento estado;

    public Mantenimiento(Equipo equipo, TipoMantenimiento tipo, String descripcionFalla,
                         Personal reportadoPor, LocalDateTime fechaLimite) {
        this.equipo = equipo;
        this.tipo = tipo;
        this.descripcionFalla = descripcionFalla;
        this.reportadoPor = reportadoPor;
        this.fechaReporte = LocalDateTime.now();
        this.fechaLimite = fechaLimite;
        this.estado = EstadoMantenimiento.PENDIENTE;
    }

    /** Se puede asignar o cambiar de mecánico mientras no esté terminado ni cancelado. */
    public boolean puedeAsignarse() {
        return estado == EstadoMantenimiento.PENDIENTE
                || estado == EstadoMantenimiento.ASIGNADO
                || estado == EstadoMantenimiento.EN_PROCESO;
    }

    /**
     * Asigna o cambia el mecánico. Un pendiente pasa a ASIGNADO;
     * uno que ya estaba en proceso conserva su estado.
     */
    public void asignar(Personal mecanico) {
        this.mecanico = mecanico;
        this.fechaAsignacion = LocalDateTime.now();
        if (estado == EstadoMantenimiento.PENDIENTE) {
            estado = EstadoMantenimiento.ASIGNADO;
        }
    }
}