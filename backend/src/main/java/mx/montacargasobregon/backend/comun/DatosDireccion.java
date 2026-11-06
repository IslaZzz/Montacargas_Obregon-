package mx.montacargasobregon.backend.comun;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Dirección de un lugar al que se lleva el equipo, como la obra de una cotización.
 * Hereda los campos de Direccion y agrega las referencias para llegar.
 */
@Entity
@Table(name = "direcciones_obra")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatosDireccion extends Direccion {

    // Ej. «Portón azul. Preguntar por el Ing. Soto.»
    @Column(length = 200)
    private String referencias;

    public DatosDireccion(String calle, String numero, String colonia, String ciudad, String estado,
                          String codigoPostal, String referencias) {
        super(calle, numero, colonia, ciudad, estado, codigoPostal);
        this.referencias = referencias;
    }
}
