package mx.montacargasobregon.backend.rentas.modelo;

/** En qué parte del recorrido va cada equipo de una renta. */
public enum FaseEquipoRenta {
    RESERVADO,
    EN_REVISION_SALIDA,
    LISTO_ENTREGA,
    EN_TRASLADO,
    EN_OPERACION,
    EN_REVISION_ENTRADA,
    COMPLETADO,
    CANCELADO
}