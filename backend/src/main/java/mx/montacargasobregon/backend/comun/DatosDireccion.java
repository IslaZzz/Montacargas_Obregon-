package mx.montacargasobregon.backend.comun;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Dirección de un lugar al que se lleva el equipo, como la obra de una cotización.
 * Tiene los mismos campos que Direccion, pero en su propia tabla.
 */
@Entity
@Table(name = "direcciones_obra")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatosDireccion extends Direccion {

    public DatosDireccion(String calle, String numero, String colonia, String ciudad, String estado,
                          String codigoPostal) {
        super(calle, numero, colonia, ciudad, estado, codigoPostal);
    }
}
