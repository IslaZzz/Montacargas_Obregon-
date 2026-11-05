package mx.montacargasobregon.backend.personal.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mx.montacargasobregon.backend.comun.Persona;

/**
 * Persona a la que se le asignan revisiones, mantenimientos o traslados.
 * No ingresa al sistema, se identifica por código
 */
@Entity
@Table(name = "personal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Personal extends Persona {

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPersonal tipo;

    public Personal(String codigo, String nombres, String apellidoPaterno,
                    String apellidoMaterno, TipoPersonal tipo) {
        super(nombres, apellidoPaterno, apellidoMaterno);
        this.codigo = codigo;
        this.tipo = tipo;
    }

    public void actualizar(String codigo, String nombres, String apellidoPaterno,
                           String apellidoMaterno, TipoPersonal tipo) {
        cambiarNombre(nombres, apellidoPaterno, apellidoMaterno);
        this.codigo = codigo;
        this.tipo = tipo;
    }
}