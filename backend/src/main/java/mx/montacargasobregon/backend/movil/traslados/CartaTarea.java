package mx.montacargasobregon.backend.movil.traslados;

/** La carta responsiva como la ve el operador en una entrega. */
public enum CartaTarea {
    SIN_GENERAR, // La oficina todavía no genera la carta: no hay nada que firmar
    PENDIENTE,   // Generada, falta la firma del cliente
    FIRMADA      // Ya se puede registrar la entrega
}