package mx.montacargasobregon.backend.equipos.modelo;

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
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;


/** Unidad física del parque de equipos. */
@Entity
@Table(name = "equipos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número interno que asigna la empresa
    @Column(name = "numero_unidad", nullable = false, unique = true, length = 50)
    private String numeroUnidad;

    // Número de serie del fabricante, el de la placa
    @Column(name = "numero_serie", nullable = false, unique = true, length = 50)
    private String numeroSerie;

    @Column(name = "anio_compra", nullable = false)
    private Integer anioCompra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private Modelo modelo;

    // STRING guarda el nombre del valor; con ORDINAL, reordenar el enum corrompe los datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEquipo estado;

    // Tabla aparte equipo_etiquetas; EAGER porque son pocas y siempre se muestran
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "equipo_etiquetas", joinColumns = @JoinColumn(name = "equipo_id"))
    @Column(name = "etiqueta", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Etiqueta> etiquetas = new HashSet<>();

    public Equipo(String numeroUnidad, String numeroSerie, Integer anioCompra, Modelo modelo) {
        this.numeroUnidad = numeroUnidad;
        this.numeroSerie = numeroSerie;
        this.anioCompra = anioCompra;
        this.modelo = modelo;
        this.estado = EstadoEquipo.OPERATIVO;
    }

    public void actualizar(String numeroUnidad, String numeroSerie, Integer anioCompra, Modelo modelo) {
        this.numeroUnidad = numeroUnidad;
        this.numeroSerie = numeroSerie;
        this.anioCompra = anioCompra;
        this.modelo = modelo;
    }

    public void cambiarEstado(EstadoEquipo nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public void darDeBaja() {
        this.estado = EstadoEquipo.BAJA;
    }

    public void asignarEtiquetas(Set<Etiqueta> etiquetas) {
        this.etiquetas.clear();
        if (etiquetas != null) {
            this.etiquetas.addAll(etiquetas);
        }
    }
}