import type { TonoChip } from "../../components/Chip";
import type { Especificacion } from "../../components/Especificaciones";

// Equivalen a los enums del backend
export type TipoRevision = "SALIDA" | "ENTRADA";
export type EstadoRevision = "PENDIENTE" | "ASIGNADO" | "EN_PROCESO" | "COMPLETADA" | "CANCELADA";
export type EstadoInspeccion = "BIEN" | "ATENCION" | "FALLA";
export type ResultadoRevision = "SIN_NOVEDAD" | "CON_OBSERVACIONES" | "REQUIERE_MANTENIMIENTO";
export type TipoCampo = "TEXTO" | "ENTERO" | "DECIMAL" | "BOOLEANO" | "FECHA" | "LISTA" | "INSPECCION" | "FOTO";

// Equivale a RevisionMecanicoResponse
export interface RevisionMecanico {
  id: number;
  unidad: string;
  tipoEquipo: string | null;
  tipo: TipoRevision;
  estado: EstadoRevision;
  fechaProgramada: string;
  rentaFolio: string;
}

// Equivale a CampoRevisionResponse: un campo que pide la categoría en la revisión
export interface CampoRevision {
  id: number;
  nombre: string;
  tipo: TipoCampo;
  unidad: string | null;
  requerido: boolean;
  opciones: string[];
}

// Equivale a ValorRevisionResponse: lo que se anotó en un campo
export interface ValorRevision {
  nombre: string;
  tipo: TipoCampo;
  unidad: string | null;
  valor: string | null;
  estado: EstadoInspeccion | null;
  detalle: string | null;
}

// Equivale a DetalleRevisionMecanicoResponse
export interface DetalleRevision extends RevisionMecanico {
  modelo: string | null;
  especificaciones: Especificacion[];
  fechaFin: string | null;
  campos: CampoRevision[];
  valores: ValorRevision[];
  resultado: ResultadoRevision | null;
  notas: string | null;
}

// Equivale a CapturaRevisionRequest
export interface Captura {
  valores: { campoId: number; valor: string | null; estado: EstadoInspeccion | null; detalle: string | null }[];
  notas: string | null;
}

// Equivale a RevisionTerminadaResponse
export interface RevisionTerminada {
  id: number;
  unidad: string;
  tipo: TipoRevision;
  resultado: ResultadoRevision;
}

export const NOMBRE_TIPO_REVISION: Record<TipoRevision, string> = {
  SALIDA: "Salida",
  ENTRADA: "Regreso",
};

export const TONO_TIPO_REVISION: Record<TipoRevision, TonoChip> = {
  SALIDA: "verde",
  ENTRADA: "amarillo",
};

export const NOMBRE_ESTADO_INSPECCION: Record<EstadoInspeccion, string> = {
  BIEN: "Bien",
  ATENCION: "Atención",
  FALLA: "Falla",
};

export const TONO_ESTADO_INSPECCION: Record<EstadoInspeccion, TonoChip> = {
  BIEN: "verde",
  ATENCION: "amarillo",
  FALLA: "rojo",
};