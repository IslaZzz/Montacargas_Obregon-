package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Descuento o precio especial de una cotización (HU-15). No tiene tabla propia: va en la de cotizaciones.
 * No cambia la tarifa del modelo ni el importe de cada equipo.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Descuento {

    @Enumerated(EnumType.STRING)
    @Column(name = "descuento_tipo", length = 20)
    private TipoDescuento tipo;

    // Porcentaje de 0 a 100, o monto en pesos, según el tipo
    @Column(name = "descuento_valor", precision = 12, scale = 2)
    private BigDecimal valor;

    public Descuento(TipoDescuento tipo, BigDecimal valor) {
        this.tipo = tipo;
        this.valor = valor;
    }
}
