package mx.montacargasobregon.backend.revisiones.modelo;

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

    // Lo que anotó el mecánico en la app, un valor por campo de revisión de la categoría.
    // Vacío si el resultado se registró desde la oficina
    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<ValorRevision> valores = new ArrayList<>();

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

    /** El mecánico abrió la revisión en la app: queda en proceso. Si ya lo estaba, no cambia nada. */
    public void iniciar() {
        if (estado == EstadoRevision.ASIGNADO) {
            estado = EstadoRevision.EN_PROCESO;
            fechaInicio = LocalDateTime.now();
        }
    }

    /** El resultado se registra cuando la revisión ya tiene mecánico y no ha terminado. */
    public boolean puedeRegistrarResultado() {
        return estado == EstadoRevision.ASIGNADO || estado == EstadoRevision.EN_PROCESO;
    }

    /** Guarda lo que reportó el mecánico y da la revisión por terminada. */
    public void registrarResultado(ResultadoRevision resultado, String notas) {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaInicio == null) {
            fechaInicio = ahora;
        }
        this.fechaFin = ahora;
        this.resultado = resultado;
        this.notas = notas;
        this.estado = EstadoRevision.COMPLETADA;
    }

    /**
     * Guarda lo que anotó el mecánico en la app y da la revisión por terminada.
     * El resultado sale de las inspecciones: una falla pide mantenimiento; una atención, solo observaciones.
     */
    public void registrarValores(List<ValorRevision> nuevos, String notas) {
        valores.clear();
        nuevos.forEach(v -> v.perteneceA(this));
        valores.addAll(nuevos);
        registrarResultado(resultadoDe(nuevos), notas);
    }

    /** Las inspecciones que quedaron en ese estado (las fallas describen el mantenimiento). */
    public List<ValorRevision> inspeccionesEn(EstadoInspeccion estadoInspeccion) {
        return valores.stream().filter(v -> v.getEstado() == estadoInspeccion).toList();
    }

    private static ResultadoRevision resultadoDe(List<ValorRevision> valores) {
        if (valores.stream().anyMatch(v -> v.getEstado() == EstadoInspeccion.FALLA)) {
            return ResultadoRevision.REQUIERE_MANTENIMIENTO;
        }
        if (valores.stream().anyMatch(v -> v.getEstado() == EstadoInspeccion.ATENCION)) {
            return ResultadoRevision.CON_OBSERVACIONES;
        }
        return ResultadoRevision.SIN_NOVEDAD;
    }
}