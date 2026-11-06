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
  correoCliente: string; // A dónde se envía
  tieneArchivo: boolean;
  puedeGenerar: boolean; // Renta Programada y carta sin firmar
  puedeEnviar: boolean; // Lo mismo y ya tiene archivo
  fechaEnvio: string | null; // Cuándo se envió la versión vigente
  correoEnvio: string | null;
  mensajeSugerido: string; // Con lo que se llena el mensaje del correo
  puedeFirmar: boolean; // Generada, sin firmar y renta Programada o En curso
  firmanteSugerido: string; // El contacto en la obra
  firma: FirmaCarta | null; // Vacía mientras no se firma
  cambios: CambioCarta[];
}

export type TipoFirma = "ELECTRONICA" | "FISICA";

// La firma registrada (OHU-05)
export interface FirmaCarta {
  tipo: TipoFirma;
  fecha: string; // Cuándo firmó el cliente
  firmante: string;
  registradaPor: string;
  rolRegistro: string;
  registradaEn: string;
  nombreArchivo: string | null;
  tipoArchivo: string | null; // application/pdf, image/jpeg o image/png
}

// Datos que faltan para generarla: dato → a quién le toca ("Cliente", "Renta" o "Equipo")
export type DatosFaltantes = Record<string, string>;
