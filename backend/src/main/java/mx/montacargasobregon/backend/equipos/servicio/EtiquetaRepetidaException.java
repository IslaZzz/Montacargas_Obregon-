package mx.montacargasobregon.backend.equipos.servicio;

public class EtiquetaRepetidaException extends RuntimeException {

    public EtiquetaRepetidaException() {
        super("Ya existe una etiqueta con ese nombre");
    }
}
