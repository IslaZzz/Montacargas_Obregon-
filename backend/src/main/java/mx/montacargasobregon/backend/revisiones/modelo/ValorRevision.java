package mx.montacargasobregon.backend.revisiones.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import mx.montacargasobregon.backend.equipos.modelo.DefinicionCampo;
import mx.montacargasobregon.backend.equipos.modelo.TipoCampo;

/**
 * Lo que anotó el mecánico en un campo de revisión de la categoría, como "1284.5" en «Horómetro»
 * o Falla en «Llantas». Guarda una copia del nombre, tipo, unidad y orden del campo,
 * para que la revisión no cambie si después se edita el campo.
 */
@Entity
@Table(name = "valores_revision",
        uniqueConstraints = @UniqueConstraint(columnNames = {"revision_id", "campo_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValorRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private Revision revision;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campo_id", nullable = false)
    private DefinicionCampo campo;

    // Copia del campo al momento de la revisión
    @Column(nullable = false, length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCampo tipo;

    @Column(length = 20)
    private String unidad;

    @Column(nullable = false)
    private int orden;

    // El dato ya normalizado ("1284.5", "3/4", "Sí"); en un campo FOTO, el id de la foto.
    // Vacío en las inspecciones y en los campos opcionales que no se llenaron
    @Column(length = 255)
    private String valor;

    // Solo en los campos INSPECCION
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private EstadoInspeccion estado;

    // Solo en una inspección en Atención o Falla: qué se encontró
    @Column(length = 255)
    private String detalle;

    public ValorRevision(DefinicionCampo campo, String valor, EstadoInspeccion estado, String detalle) {
        this.campo = campo;
        this.nombre = campo.getNombre();
        this.tipo = campo.getTipo();
        this.unidad = campo.getUnidad();
        this.orden = campo.getOrden();
        this.valor = valor;
        this.estado = estado;
        this.detalle = detalle;
    }

    void perteneceA(Revision revision) {
        this.revision = revision;
    }

    /** "Llantas: desgaste en llanta delantera derecha" */
    public String describir() {
        return detalle == null ? nombre : nombre + ": " + detalle;
    }
}