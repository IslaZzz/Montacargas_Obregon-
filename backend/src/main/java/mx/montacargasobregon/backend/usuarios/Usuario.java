package mx.montacargasobregon.backend.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.comun.Persona;

/** Persona que entra al sistema. */
@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends Persona {

    @Column(name = "numero_empleado", nullable = false, unique = true, length = 20)
    private String numeroEmpleado;

    // Será el identificador para entrar al sistema
    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUsuario tipo;

    public Usuario(String numeroEmpleado, String nombres, String apellidoPaterno,
                   String apellidoMaterno, String correo, TipoUsuario tipo) {
        super(nombres, apellidoPaterno, apellidoMaterno);
        this.numeroEmpleado = numeroEmpleado;
        this.correo = correo;
        this.tipo = tipo;
    }

    public void actualizar(String numeroEmpleado, String nombres, String apellidoPaterno,
                           String apellidoMaterno, String correo, TipoUsuario tipo) {
        cambiarNombre(nombres, apellidoPaterno, apellidoMaterno);
        this.numeroEmpleado = numeroEmpleado;
        this.correo = correo;
        this.tipo = tipo;
    }
}