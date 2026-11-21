package mx.montacargasobregon.backend.mantenimientos.modelo;

/** De dónde salió una tarea del mantenimiento. */
public enum OrigenTarea {
    ASIGNADA, // Venía con el mantenimiento: un hallazgo de la revisión o la falla reportada
    AGREGADA  // La agregó el mecánico al encontrar algo que no estaba contemplado
}