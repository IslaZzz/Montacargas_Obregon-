package mx.montacargasobregon.backend.equipos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Característica operativa que puede tener una unidad, como «Uso en interiores». Gerencia puede agregar más. */
@Entity
@Table(name = "etiquetas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Etiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    public Etiqueta(String nombre) {
        this.nombre = nombre;
    }
}
