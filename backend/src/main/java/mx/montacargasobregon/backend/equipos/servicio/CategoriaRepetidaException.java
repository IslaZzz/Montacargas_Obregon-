package mx.montacargasobregon.backend.equipos.servicio;

public class CategoriaRepetidaException extends RuntimeException {

    public CategoriaRepetidaException() {
        super("Ya existe una categoría con ese nombre");
    }
}
