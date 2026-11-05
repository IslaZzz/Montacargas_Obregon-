package mx.montacargasobregon.backend.clientes.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.clientes.modelo.DireccionFiscal;

/**
 * Razón social de un cliente, los datos fiscales para facturar.
 */
@Entity
@Table(name = "razones_sociales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RazonSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 13)
    private String rfc;

    // Cada razón social tiene exactamente una dirección fiscal
    // Se guarda junto con la razón social, pero no se borra en cascada.
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, optional = false)
    @JoinColumn(name = "direccion_fiscal_id", nullable = false)
    private DireccionFiscal direccionFiscal;

    public RazonSocial(String nombre, String rfc, DireccionFiscal direccionFiscal) {
        this.nombre = nombre;
        this.rfc = rfc;
        this.direccionFiscal = direccionFiscal;
    }
}
