package mx.montacargasobregon.backend.equipos.consulta;

/** Estado con el que se muestra un equipo: junta su estado operativo con la renta en la que está. */
public enum EstadoConsultaEquipo {
    DISPONIBLE,        // Operativo y sin renta
    EN_PREPARACION,    // En una renta, pero todavía no llega al cliente o ya regresó y falta revisarlo
    EN_RENTA,          // Con el cliente
    EN_MANTENIMIENTO,
    FUERA_SERVICIO
}