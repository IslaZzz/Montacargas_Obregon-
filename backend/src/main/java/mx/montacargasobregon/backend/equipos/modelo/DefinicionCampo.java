package mx.montacargasobregon.backend.equipos.modelo;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Campo que pide una categoría. Los de MODELO se llenan al registrar el modelo, como «Capacidad de carga»,
 * y cada modelo guarda su valor en ValorEspecificacion. Los de REVISION se llenan en cada revisión de una
 * unidad, como «Horómetro» o «Llantas», y cada revisión guarda su valor en ValorRevision.
 */
@Entity
@Table(name = "definiciones_campo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"categoria_id", "nombre"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DefinicionCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCampo tipo;

    // Ej. "kg" o "m"; vacío si el campo no lleva unidad
    @Column(length = 20)
    private String unidad;

    @Column(nullable = false)
    private boolean requerido;

    // Posición del campo en el formulario
    @Column(nullable = false)
    private int orden;

    // Dónde se llena: al registrar el modelo o en cada revisión
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CapturaCampo capturaCampo;

    // Solo para los campos LISTA: las opciones que se pueden elegir
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "definicion_campo_opciones", joinColumns = @JoinColumn(name = "campo_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "opcion", nullable = false, length = 60)
    private List<String> opciones = new ArrayList<>();

    /** Campo que se llena al registrar el modelo. */
    public DefinicionCampo(Categoria categoria, String nombre, TipoCampo tipo, String unidad,
                           boolean requerido, int orden, List<String> opciones) {
        this(categoria, nombre, tipo, unidad, requerido, orden, opciones, CapturaCampo.MODELO);
    }

    public DefinicionCampo(Categoria categoria, String nombre, TipoCampo tipo, String unidad,
                           boolean requerido, int orden, List<String> opciones, CapturaCampo capturaCampo) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.tipo = tipo;
        this.unidad = unidad;
        this.requerido = requerido;
        this.orden = orden;
        this.opciones = new ArrayList<>(opciones);
        this.capturaCampo = capturaCampo;
    }

    public boolean seCapturaEn(CapturaCampo donde) {
        return capturaCampo == donde;
    }
}