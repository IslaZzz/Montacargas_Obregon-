package mx.montacargasobregon.backend.rentas.modelo;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.equipos.modelo.Equipo;

/** Un equipo dentro de una renta, con la fase en la que va. */
@Entity
@Table(
        name = "equipos_renta",
        // Un mismo equipo no puede aparecer dos veces en la misma renta
        uniqueConstraints = @UniqueConstraint(columnNames = { "renta_id", "equipo_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipoRenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "renta_id", nullable = false)
    private Renta renta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private FaseEquipoRenta fase;

    // Sin "public": solo Renta.agregarEquipo lo crea, para que siempre quede ligado a su renta
    EquipoRenta(Renta renta, Equipo equipo) {
        this.renta = renta;
        this.equipo = equipo;
        this.fase = FaseEquipoRenta.RESERVADO;
    }

    public void cambiarFase(FaseEquipoRenta nuevaFase) {
        this.fase = nuevaFase;
    }
}