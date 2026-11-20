package mx.montacargasobregon.backend.equipos.modelo;

/** Tipo de dato de un campo de una categoría. */
public enum TipoCampo {
    TEXTO,
    ENTERO,
    DECIMAL,
    BOOLEANO,
    FECHA,
    LISTA,
    INSPECCION, // Solo en revisión. Bien, Atención o Falla, con detalle si no está bien
    FOTO        // Solo en revisión. Una foto con el nombre del campo
}