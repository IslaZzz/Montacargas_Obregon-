package mx.montacargasobregon.backend.revisiones.modelo;

/** Etapa en la que va una revisión. Mismos estados que mantenimientos y traslados. */
public enum EstadoRevision {
    PENDIENTE,   // Con fecha, sin mecánico. Le aparece al auxiliar
    ASIGNADO,    // Ya tiene mecánico, no ha empezado
    EN_PROCESO,  // El mecánico está revisando
    COMPLETADA,
    CANCELADA
}