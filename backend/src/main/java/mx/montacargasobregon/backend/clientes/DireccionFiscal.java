package mx.montacargasobregon.backend.clientes;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.comun.Direccion;

/**
 * Dirección fiscal de una razón social. Hereda todos los campos de Direccion.
 */
@Entity
@Table(name = "direcciones_fiscales")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DireccionFiscal extends Direccion {

    public DireccionFiscal(String calle, String numero, String colonia, String ciudad, String codigoPostal) {
        super(calle, numero, colonia, ciudad, codigoPostal);
    }
}
