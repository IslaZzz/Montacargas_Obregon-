package mx.montacargasobregon.backend.contratos.modelo;

/**
 * Situación del contrato (HU-23). Nace Apartado al aceptar la cotización y vence solo si no se paga el
 * anticipo a tiempo. Contratado llega con el anticipo (HU-24) y Cancelado con HU-26.
 */
public enum EstadoContrato {
    APARTADO,
    APARTADO_VENCIDO
}
