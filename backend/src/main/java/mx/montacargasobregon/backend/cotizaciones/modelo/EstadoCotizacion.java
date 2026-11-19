package mx.montacargasobregon.backend.cotizaciones.modelo;

/**
 * Situación de una cotización frente al cliente.
 */
public enum EstadoCotizacion {
    BORRADOR,
    ENVIADA,
    ACEPTADA,
    NO_ACEPTADA,
    EN_AUTORIZACION,
    RECHAZADA,
    VENCIDA;

    /** Indica si desde este estado se puede pasar al estado destino. */
    public boolean puedeCambiarA(EstadoCotizacion destino) {
        return switch (this) {
            case BORRADOR -> destino == ENVIADA || destino == EN_AUTORIZACION;
            case ENVIADA -> destino == ACEPTADA || destino == NO_ACEPTADA;
            case EN_AUTORIZACION -> destino == BORRADOR || destino == RECHAZADA;
            case ACEPTADA, NO_ACEPTADA, RECHAZADA, VENCIDA -> false; // Son finales, ya no cambian
        };
    }
}
