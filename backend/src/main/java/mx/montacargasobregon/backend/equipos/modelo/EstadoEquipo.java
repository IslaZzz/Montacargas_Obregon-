package mx.montacargasobregon.backend.equipos.modelo;

/** Situación operativa en la que se encuentra un equipo. */
public enum EstadoEquipo {
    EN_REVISION_ENTRADA, // Recién registrado: Operativo lo revisa y lo pone disponible (HU-02)
    OPERATIVO,
    EN_MANTENIMIENTO,
    FUERA_SERVICIO,
    BAJA
}
