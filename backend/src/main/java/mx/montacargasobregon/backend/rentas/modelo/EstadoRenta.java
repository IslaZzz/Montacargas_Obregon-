package mx.montacargasobregon.backend.rentas.modelo;

/** Situación de la renta completa. */
public enum EstadoRenta {
    PROGRAMADA,  // Contratada, todavía no se entrega
    EN_CURSO,    // Los equipos ya están con el cliente
    CERRADA,     // Ya se recogieron
    CANCELADA
}