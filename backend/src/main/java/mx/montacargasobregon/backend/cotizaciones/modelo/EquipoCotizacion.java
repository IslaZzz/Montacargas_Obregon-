package mx.montacargasobregon.backend.cotizaciones.modelo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Un equipo dentro de una cotización, con el precio con el que se cotizó. */
@Entity
@Table(name = "equipos_cotizacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipoCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    // Tarifa por día, copiada del modelo al cotizar: si la tarifa cambia después, la cotización conserva su precio
    @Column(name = "tarifa_dia", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifa;

    // Días de renta × tarifa por día (HU-13)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    public EquipoCotizacion(Equipo equipo, BigDecimal tarifa, BigDecimal importe) {
        this.equipo = equipo;
        this.tarifa = tarifa;
        this.importe = importe;
    }
}
