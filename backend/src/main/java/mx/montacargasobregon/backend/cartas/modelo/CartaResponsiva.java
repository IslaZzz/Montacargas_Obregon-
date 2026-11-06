package mx.montacargasobregon.backend.cartas.modelo;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
import mx.montacargasobregon.backend.rentas.modelo.Renta;

/**
 * Carta responsiva de una renta.
 * Por ahora solo guarda en qué va (la usan los avisos de apartado próximo, OHU-24).
 * Generar el PDF, enviarlo y registrar la firma es de OHU-04: esa historia puede agregar lo que necesite aquí.
 * Si una renta no tiene registro, su carta está SIN_GENERAR.
 */
@Entity
@Table(name = "cartas_responsivas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartaResponsiva {

    // Misma zona que cotizaciones: la empresa trabaja con la hora de Sonora
    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Hermosillo");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Una sola carta por renta
    @OneToOne(optional = false)
    @JoinColumn(name = "renta_id", nullable = false, unique = true)
    private Renta renta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCarta estado;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    /** Crea la carta recién generada. */
    public CartaResponsiva(Renta renta) {
        this.renta = renta;
        this.estado = EstadoCarta.GENERADA;
        this.fechaGeneracion = LocalDateTime.now(ZONA_HORARIA);
    }

    public void cambiarEstado(EstadoCarta nuevoEstado) {
        this.estado = nuevoEstado;
    }
}
