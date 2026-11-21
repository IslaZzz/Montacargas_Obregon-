package mx.montacargasobregon.backend.mantenimientos.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una cosa por atender dentro de un mantenimiento: un hallazgo de la revisión, la falla reportada,
 * o algo que agregó el mecánico. El mantenimiento se finaliza cuando todas están atendidas.
 */
@Entity
@Table(name = "tareas_mantenimiento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TareaMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mantenimiento_id", nullable = false)
    private Mantenimiento mantenimiento;

    @Column(nullable = false, length = 100)
    private String titulo;

    // Lo que se encontró, tal como llegó (solo lectura para el mecánico)
    @Column(length = 255)
    private String hallazgo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrigenTarea origen;

    // Solo en las agregadas
    @Column(length = 100)
    private String componente;

    @Column(name = "accion_sugerida", length = 255)
    private String accionSugerida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoTarea estado;

    // Lo que registra el mecánico al atenderla
    @Column(name = "trabajo_realizado", length = 500)
    private String trabajoRealizado;

    @Column(length = 255)
    private String refacciones;

    @Column(name = "tiempo_invertido", length = 50)
    private String tiempoInvertido;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "atendida_en")
    private LocalDateTime atendidaEn;

    // Cuando el mecánico no pudo completarla y avisó al auxiliar
    @Column(length = 500)
    private String incidencia;

    @Column(name = "incidencia_en")
    private LocalDateTime incidenciaEn;

    TareaMantenimiento(Mantenimiento mantenimiento, String titulo, String hallazgo, OrigenTarea origen,
                       String componente, String accionSugerida) {
        this.mantenimiento = mantenimiento;
        this.titulo = titulo;
        this.hallazgo = hallazgo;
        this.origen = origen;
        this.componente = componente;
        this.accionSugerida = accionSugerida;
        this.estado = EstadoTarea.PENDIENTE;
    }

    /** Registra o corrige lo que se hizo. Una incidencia anterior queda resuelta. */
    public void atender(String trabajoRealizado, String refacciones, String tiempoInvertido, String observaciones) {
        this.trabajoRealizado = trabajoRealizado;
        this.refacciones = refacciones;
        this.tiempoInvertido = tiempoInvertido;
        this.observaciones = observaciones;
        this.atendidaEn = LocalDateTime.now();
        this.estado = EstadoTarea.ATENDIDA;
        this.incidencia = null;
        this.incidenciaEn = null;
    }

    /** El mecánico no pudo completarla: queda pendiente con el aviso para el auxiliar. */
    public void reportarIncidencia(String mensaje) {
        this.incidencia = mensaje;
        this.incidenciaEn = LocalDateTime.now();
    }

    public boolean atendida() {
        return estado == EstadoTarea.ATENDIDA;
    }
}