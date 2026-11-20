package mx.montacargasobregon.backend.revisiones.modelo;

/** Cómo encontró el mecánico un campo de inspección en una revisión. */
public enum EstadoInspeccion {
    BIEN,
    ATENCION, // Funciona, pero hay que vigilarlo o atenderlo
    FALLA     // No puede salir así: nace un mantenimiento
}