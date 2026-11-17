package mx.montacargasobregon.backend.equipos.servicio;

import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Datos del modelo que no se pueden guardar, con el error de cada campo. */
@Getter
public class DatosModeloException extends RuntimeException {

    private final HttpStatus estado;
    private final Map<String, String> errores;

    public DatosModeloException(HttpStatus estado, Map<String, String> errores) {
        super("Hay datos por corregir.");
        this.estado = estado;
        this.errores = errores;
    }
}
