// Carta responsiva de una renta (OHU-04), como la manda GET /api/rentas/{id}/carta

export type EstadoCarta = "SIN_GENERAR" | "GENERADA" | "ENVIADA" | "FIRMADA";

export type EstadoRenta = "PROGRAMADA" | "EN_CURSO" | "CERRADA" | "CANCELADA";

// Un dato de la renta que cambió después de generar la carta
export interface CambioCarta {
  campo: string;
  antes: string;
  ahora: string;
}

export interface Carta {
  rentaId: number;
  folio: string;
  estadoRenta: EstadoRenta;
  estado: EstadoCarta;
  version: number | null; // null mientras no se genera
  nombreArchivo: string | null;
  fechaGeneracion: string | null;
  correoCliente: string; // A dónde se enviaría
  tieneArchivo: boolean;
  puedeGenerar: boolean; // Renta Programada y carta sin firmar
  cambios: CambioCarta[];
}

// Datos que faltan para generarla: dato → a quién le toca ("Cliente", "Renta" o "Equipo")
export type DatosFaltantes = Record<string, string>;
