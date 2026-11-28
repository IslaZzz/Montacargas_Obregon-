package mx.montacargasobregon.backend.equipos.consulta;

/** Estado con el que se muestra un equipo: junta su estado operativo con la renta en la que está. */
public enum EstadoConsultaEquipo {
    DISPONIBLE,        // Operativo y sin renta
    EN_PREPARACION,    // En una renta, pero todavía no sale o ya regresó y falta revisarlo
    EN_TRASLADO,       // En camino: hacia el cliente o de regreso a la oficina
    EN_RENTA,          // Con el cliente
    EN_REVISION_ENTRADA, // Recién registrado; Operativo todavía no lo pone disponible
    EN_MANTENIMIENTO,
    FUERA_SERVICIO
}