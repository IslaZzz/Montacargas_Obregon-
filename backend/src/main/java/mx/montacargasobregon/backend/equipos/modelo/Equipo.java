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
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

    // Año de compra o fabricación; puede no conocerse
    @Column(name = "anio_compra")
    private Integer anioCompra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private Modelo modelo;

    // STRING guarda el nombre del valor; con ORDINAL, reordenar el enum corrompe los datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEquipo estado;

    // Muchos a muchos en equipos_etiquetas; EAGER porque son pocas y siempre se muestran
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "equipos_etiquetas",
            joinColumns = @JoinColumn(name = "equipo_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id"))
    private Set<Etiqueta> etiquetas = new HashSet<>();

    /** Un equipo nuevo entra «En revisión de entrada»; Operativo lo pone disponible. */
    public Equipo(String numeroUnidad, String numeroSerie, Integer anioCompra, Modelo modelo) {
        this.numeroUnidad = numeroUnidad;
        this.numeroSerie = numeroSerie;
        this.anioCompra = anioCompra;
        this.modelo = modelo;
        this.estado = EstadoEquipo.EN_REVISION_ENTRADA;
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
