package mx.montacargasobregon.backend.mantenimientos.modelo;

/** Etapa en la que va un mantenimiento. Mismos estados que revisiones y traslados. */
public enum EstadoMantenimiento {
    PENDIENTE,   // Con fecha, sin mecánico. Le aparece al auxiliar
    ASIGNADO,    // Ya tiene mecánico, no ha empezado
    EN_PROCESO,  // El mecánico está trabajando en él
    COMPLETADO,
    CANCELADO
}