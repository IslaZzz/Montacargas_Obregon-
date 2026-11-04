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

/** Agrupación de modelos por tipo de máquina: montacargas, generador, etc. */
@Entity
@Table(name = "categorias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    public Categoria(String nombre) {
        this.nombre = nombre;
    }
}