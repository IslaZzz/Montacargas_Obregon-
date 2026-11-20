package mx.montacargasobregon.backend.equipos.servicio;

import lombok.Getter;

/** El número de unidad o el de serie ya lo tiene otro equipo. Dice cuál campo es. */
@Getter
public class EquipoRepetidoException extends RuntimeException {

    private final String campo;

    public EquipoRepetidoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }
}
