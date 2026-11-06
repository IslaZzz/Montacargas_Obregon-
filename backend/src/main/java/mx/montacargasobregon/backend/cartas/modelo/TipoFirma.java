package mx.montacargasobregon.backend.cartas.modelo;

/** Cómo firmó el cliente la carta responsiva (OHU-05). */
public enum TipoFirma {
    /** Firmó el PDF que se le envió por correo y lo regresó. */
    ELECTRONICA,
    /** Firmó la carta impresa; se sube una foto o un escaneo como evidencia. */
    FISICA
}
