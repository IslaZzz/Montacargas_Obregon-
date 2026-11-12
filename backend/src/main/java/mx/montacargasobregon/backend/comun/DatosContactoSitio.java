package mx.montacargasobregon.backend.comun;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Quién recibe en el lugar de entrega y su teléfono.
 * No tiene tabla propia: sus columnas van en la tabla de quien lo usa.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatosContactoSitio {

    private String nombre;

    // 10 dígitos, sin espacios
    private String telefono;

    public DatosContactoSitio(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }
}
