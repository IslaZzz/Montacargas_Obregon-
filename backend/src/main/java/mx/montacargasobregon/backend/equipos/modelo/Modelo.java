package mx.montacargasobregon.backend.equipos.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Vacía hasta que se captura en «Tarifas» (HU-04); el modelo se registra sin ella (HU-36)
    @Column(name = "tarifa_dia", precision = 10, scale = 2)
    private BigDecimal tarifaDia;

    // Varios modelos pertenecen a una misma categoría
    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // Valores de los campos que pide su categoría (capacidad, altura del mástil...)
    @OneToMany(mappedBy = "modelo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ValorEspecificacion> especificaciones = new ArrayList<>();

    public Modelo(String nombre, String marca, String codigo, String descripcion,
                  BigDecimal tarifaDia, Categoria categoria) {
        this.nombre = nombre;
        this.marca = marca;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tarifaDia = tarifaDia;
        this.categoria = categoria;
    }

    /** Cambia sus datos. La tarifa no cambia aquí: se captura en «Tarifas». */
    public void actualizar(String nombre, String marca, String codigo, String descripcion,
                           Categoria categoria) {
        this.nombre = nombre;
        this.marca = marca;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.categoria = categoria;
    }

    /**
     * Deja sus especificaciones con estos valores, uno por campo; los campos que no vienen se quitan.
     * Los que ya tenía se cambian en su lugar, para no repetir el par modelo-campo al guardar.
     */
    public void cambiarEspecificaciones(Map<DefinicionCampo, String> valores) {
        Map<DefinicionCampo, String> pendientes = new HashMap<>(valores);
        especificaciones.removeIf(v -> !pendientes.containsKey(v.getCampo()));
        especificaciones.forEach(v -> v.cambiarValor(pendientes.remove(v.getCampo())));
        pendientes.forEach((campo, valor) -> especificaciones.add(new ValorEspecificacion(this, campo, valor)));
    }
}
