package mx.montacargasobregon.backend.mantenimientos.modelo;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;
import mx.montacargasobregon.backend.personal.modelo.Personal;
import mx.montacargasobregon.backend.revisiones.modelo.EstadoInspeccion;
import mx.montacargasobregon.backend.revisiones.modelo.Revision;
import mx.montacargasobregon.backend.revisiones.modelo.ValorRevision;

/**
 * Reparación o servicio sobre un equipo.
 * La falla, quién la reportó y cuándo viven aquí porque un preventivo no viene de ninguna revisión.
 * El trabajo se divide en tareas: las fallas y puntos en atención de la revisión, o la falla reportada,
 * más lo que agregue el mecánico.
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

    // La revisión en la que se detectó la falla; vacío en los preventivos
    @ManyToOne
    @JoinColumn(name = "revision_id")
    private Revision revision;

    @OneToMany(mappedBy = "mantenimiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<TareaMantenimiento> tareas = new ArrayList<>();

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

    /**
     * Mantenimiento por una falla encontrada en una revisión.
     * Lo reporta el mecánico de la revisión y queda ligado a ella.
     */
    public static Mantenimiento deRevision(Revision revision, String descripcionFalla, LocalDateTime fechaLimite) {
        Mantenimiento m = new Mantenimiento(revision.getEquipoRenta().getEquipo(), TipoMantenimiento.CORRECTIVO,
                descripcionFalla, revision.getMecanico(), fechaLimite);
        m.revision = revision;
        // Cada inspección en falla o en atención de la revisión se vuelve una tarea
        for (EstadoInspeccion estado : List.of(EstadoInspeccion.FALLA, EstadoInspeccion.ATENCION)) {
            for (ValorRevision inspeccion : revision.inspeccionesEn(estado)) {
                m.tareas.add(new TareaMantenimiento(m, inspeccion.getNombre(), inspeccion.getDetalle(),
                        OrigenTarea.ASIGNADA, null, null));
            }
        }
        return m;
    }

    /** Se marca terminado cuando ya tiene mecánico y no ha terminado. */
    public boolean puedeTerminarse() {
        return estado == EstadoMantenimiento.ASIGNADO || estado == EstadoMantenimiento.EN_PROCESO;
    }

    /** El mecánico avisó que el equipo quedó reparado. */
    public void terminar() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaInicio == null) {
            fechaInicio = ahora;
        }
        this.fechaFin = ahora;
        this.estado = EstadoMantenimiento.COMPLETADO;
    }

    /**
     * El mecánico abrió el mantenimiento en la app: queda en proceso.
     * Si no traía tareas (se reportó desde la oficina), la falla reportada se vuelve su tarea.
     */
    public void iniciar() {
        if (tareas.isEmpty()) {
            tareas.add(new TareaMantenimiento(this, "Falla reportada", descripcionFalla,
                    OrigenTarea.ASIGNADA, null, null));
        }
        if (estado == EstadoMantenimiento.ASIGNADO) {
            estado = EstadoMantenimiento.EN_PROCESO;
            fechaInicio = LocalDateTime.now();
        }
    }

    /** Algo que el mecánico encontró y no estaba contemplado. */
    public TareaMantenimiento agregarTarea(String titulo, String componente, String accionSugerida) {
        TareaMantenimiento tarea = new TareaMantenimiento(this, titulo, null, OrigenTarea.AGREGADA,
                componente, accionSugerida);
        tareas.add(tarea);
        return tarea;
    }

    /** Se puede finalizar desde la app cuando todas sus tareas están atendidas. */
    public boolean tareasCompletas() {
        return !tareas.isEmpty() && tareas.stream().allMatch(TareaMantenimiento::atendida);
    }
}