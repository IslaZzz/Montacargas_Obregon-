package mx.montacargasobregon.backend.cotizaciones.modelo;

/**
 * Situación de una cotización frente al cliente.
 * En Delivery S1 solo existen estos cuatro. En autorización, Vencida y Rechazada llegan con HU-16, HU-17 y HU-32.
 */
public enum EstadoCotizacion {
    BORRADOR,
    ENVIADA,
    ACEPTADA,
    NO_ACEPTADA;

    /** Indica si desde este estado se puede pasar al estado destino. */
    public boolean puedeCambiarA(EstadoCotizacion destino) {
        return switch (this) {
            case BORRADOR -> destino == ENVIADA;
            case ENVIADA -> destino == ACEPTADA || destino == NO_ACEPTADA;
            case ACEPTADA, NO_ACEPTADA -> false; // Son finales: ya no cambian
        };
    }
}
