package mx.montacargasobregon.backend.avisos.modelo;

/** Qué pasó en la operación. Los primeros cuatro le llegan a la oficina; el último, al operador. */
public enum TipoAvisoOperacion {
    TRASLADO_INICIADO,      // El operador salió con los equipos de una entrega
    ENTREGA_REGISTRADA,     // Los equipos quedaron con el cliente
    RECOLECCION_CONFIRMADA, // El operador recogió los equipos y van de regreso
    EQUIPOS_RECIBIDOS,      // Los equipos llegaron a la oficina y esperan su revisión
    TAREA_REASIGNADA        // La oficina le pasó la tarea a otro operador
}