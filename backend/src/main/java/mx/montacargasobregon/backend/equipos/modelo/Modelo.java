package mx.montacargasobregon.backend.equipos.modelo;

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
import java.math.BigDecimal;

/** Producto del catálogo. De un mismo modelo puede haber varias unidades físicas. */
@Entity
@Table(name = "modelos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Modelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String marca;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "tarifa_dia", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaDia;

    // Varios modelos pertenecen a una misma categoría
    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    public Modelo(String nombre, String marca, String codigo, String descripcion,
                  BigDecimal tarifaDia, Categoria categoria) {
        this.nombre = nombre;
        this.marca = marca;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tarifaDia = tarifaDia;
        this.categoria = categoria;
    }
}