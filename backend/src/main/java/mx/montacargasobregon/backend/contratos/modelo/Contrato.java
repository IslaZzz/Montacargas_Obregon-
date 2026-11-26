package mx.montacargasobregon.backend.contratos.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.cotizaciones.modelo.Cotizacion;

/**
 * Contrato que se genera solo al aceptar una cotización (HU-23).
 * Nace Apartado: sus equipos siguen disponibles para otras cotizaciones y todavía no hay renta.
 * Si el anticipo no se paga antes de la fecha límite, el apartado vence solo.
 */
@Entity
@Table(name = "contratos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contrato {

    // El anticipo es el 30 % del total con IVA
    public static final BigDecimal PORCENTAJE_ANTICIPO = new BigDecimal("0.30");
    // Días para pagar el anticipo desde que se aceptó la cotización
    public static final int DIAS_PARA_ANTICIPO = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ej. CON-0001
    @Column(nullable = false, unique = true, length = 20)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContrato estado;

    // Cliente, razón social, equipos, fechas, lugar y precio se leen de la cotización: aceptada ya no cambia.
    // Cada cotización genera un solo contrato
    @OneToOne(optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false, unique = true)
    private Cotizacion cotizacion;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaHoraGeneracion;

    // El día de la aceptación + DIAS_PARA_ANTICIPO. Ese día, si no se pagó, el apartado vence
    @Column(name = "fecha_limite_anticipo", nullable = false)
    private LocalDate fechaLimiteAnticipo;

    // Se guarda para que el detalle, el PDF y los pagos (HU-24) usen siempre el mismo monto
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal anticipo;

    /** Genera el contrato de la cotización aceptada en ese momento. */
    public Contrato(String folio, Cotizacion cotizacion, LocalDateTime generacion) {
        this.folio = folio;
        this.cotizacion = cotizacion;
        this.fechaHoraGeneracion = generacion;
        this.fechaLimiteAnticipo = generacion.toLocalDate().plusDays(DIAS_PARA_ANTICIPO);
        this.anticipo = cotizacion.getTotal().multiply(PORCENTAJE_ANTICIPO).setScale(2, RoundingMode.HALF_UP);
        this.estado = EstadoContrato.APARTADO;
    }

    /**
     * True si ya llegó la fecha límite: aceptada el 9, el anticipo se paga antes del 12 y el 12 vence
     * («a los 3 días sin anticipo»).
     */
    public boolean plazoDeAnticipoTerminado(LocalDate hoy) {
        return !hoy.isBefore(fechaLimiteAnticipo);
    }

    /** No se pagó el anticipo a tiempo. Quien llama revisa que siga Apartado. */
    public void vencerApartado() {
        this.estado = EstadoContrato.APARTADO_VENCIDO;
    }
}
