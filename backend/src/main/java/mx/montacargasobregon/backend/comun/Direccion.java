package mx.montacargasobregon.backend.comun;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Datos comunes de cualquier dirección. No tiene tabla propia, cada subclase guarda estos campos en su tabla.
 */
@MappedSuperclass  // Sus campos pasan a las tablas de las clases hijas
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String calle;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(nullable = false, length = 100)
    private String colonia;

    @Column(nullable = false, length = 100)
    private String ciudad;

    // Opcional en la base: la dirección fiscal (HU-06) todavía no lo pide
    @Column(length = 50)
    private String estado;

    @Column(name = "codigo_postal", nullable = false, length = 5)
    private String codigoPostal;

    protected Direccion(String calle, String numero, String colonia, String ciudad, String codigoPostal) {
        this(calle, numero, colonia, ciudad, null, codigoPostal);
    }

    protected Direccion(String calle, String numero, String colonia, String ciudad, String estado,
                        String codigoPostal) {
        this.calle = calle;
        this.numero = numero;
        this.colonia = colonia;
        this.ciudad = ciudad;
        this.estado = estado;
        this.codigoPostal = codigoPostal;
    }
}
