package mx.montacargasobregon.backend.clientes;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cliente de la empresa
 */
@Entity
@Table(name = "clientes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // el constructor vacío que exige JPA
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "apellido_paterno", nullable = false, length = 50)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 50)   // Opcional
    private String apellidoMaterno;

    @Column(nullable = false, length = 100)
    private String empresa;

    @Column(nullable = false, length = 10)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String correo;

    // Un cliente tiene una o varias razones sociales.
    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "cliente_id")
    private List<RazonSocial> razonesSociales = new ArrayList<>();

    public Cliente(String nombre, String apellidoPaterno, String apellidoMaterno,
                   String empresa, String telefono, String correo) {
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.empresa = empresa;
        this.telefono = telefono;
        this.correo = correo;
    }

    public void agregarRazonSocial(RazonSocial razonSocial) {
        razonesSociales.add(razonSocial);
    }
}
