package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Descuento o precio especial de una cotización (HU-15), con el motivo que se le da a gerencia.
 * Una cotización tiene uno o ninguno (0..1). Si no hay descuento, la cotización no tiene registro aquí.
 * No cambia la tarifa del modelo ni el importe de cada equipo.
 */
@Entity
@Table(name = "descuentos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Descuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PORCENTAJE o MONTO. NINGUNO no se guarda: en ese caso la cotización no tiene descuento
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDescuento tipo;

    // Porcentaje (mayor a 0 y menor a 100) o monto en pesos (mayor a 0 y menor al subtotal), según el tipo
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 200)
    private String motivo;

    public Descuento(TipoDescuento tipo, BigDecimal valor, String motivo) {
        this.tipo = tipo;
        this.valor = valor;
        this.motivo = motivo;
    }

    /**
     * Porcentaje: mayor a 0 y menor a 100. Monto: mayor a 0 y menor al subtotal
     * (equipos + traslados).
     */
    public static boolean valorValido(TipoDescuento tipo, BigDecimal valor, BigDecimal subtotal) {
        if (valor == null || valor.signum() <= 0) {
            return false;
        }
        BigDecimal limite = tipo == TipoDescuento.PORCENTAJE ? new BigDecimal("100") : subtotal;
        return valor.compareTo(limite) < 0;
    }
}
