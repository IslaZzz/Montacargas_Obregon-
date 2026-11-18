package mx.montacargasobregon.backend.equipos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Valor que un modelo tiene en un campo de su categoría, como "3000" en «Capacidad de carga». */
@Entity
@Table(name = "valores_especificacion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"modelo_id", "campo_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValorEspecificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private Modelo modelo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "campo_id", nullable = false)
    private DefinicionCampo campo;

    @Column(nullable = false, length = 120)
    private String valor;

    ValorEspecificacion(Modelo modelo, DefinicionCampo campo, String valor) {
        this.modelo = modelo;
        this.campo = campo;
        this.valor = valor;
    }

    void cambiarValor(String valor) {
        this.valor = valor;
    }
}
