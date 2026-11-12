package mx.montacargasobregon.backend.cotizaciones.modelo;

/**
 * Situación de una cotización frente al cliente.
 * En Delivery S1 se usan Borrador, Enviada, Aceptada y No aceptada. Los cambios hacia En autorización,
 * Rechazada y Vencida llegan con HU-16, HU-32 y HU-17; mientras tanto, ningún estado pasa a ellos.
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
            case BORRADOR -> destino == ENVIADA;
            case ENVIADA -> destino == ACEPTADA || destino == NO_ACEPTADA;
            case ACEPTADA, NO_ACEPTADA -> false; // Son finales: ya no cambian
            case EN_AUTORIZACION, RECHAZADA, VENCIDA -> false; // Sus cambios llegan con HU-16, HU-32 y HU-17
        };
    }
}
