package mx.montacargasobregon.backend.cartas.modelo;

/** En qué va la carta responsiva de una renta. */
public enum EstadoCarta {
    SIN_GENERAR, // La renta todavía no tiene carta
    GENERADA,    // Ya se creó el PDF, falta mandarlo al cliente
    ENVIADA,     // Se mandó al cliente, falta que la firme
    FIRMADA      // El cliente ya la firmó: no hay nada pendiente
}
