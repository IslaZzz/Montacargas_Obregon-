import type { Domicilio } from "../../utils/domicilio";

// Equivalen a los enums del backend
export type TipoTraslado = "ENTREGA" | "RECOLECCION";
export type EstadoTraslado = "PROGRAMADO" | "EN_CURSO" | "COMPLETADO" | "CANCELADO";
export type EstadoRenta = "PROGRAMADA" | "EN_CURSO" | "CERRADA" | "CANCELADA";
export type FaseEquipo =
  | "RESERVADO"
  | "EN_REVISION_SALIDA"
  | "LISTO_ENTREGA"
  | "EN_TRASLADO"
  | "EN_OPERACION"
  | "EN_REVISION_ENTRADA"
  | "COMPLETADO"
  | "CANCELADO";

// Equivale a TareaResponse
export interface Tarea {
  id: number;
  folio: string; // TR-0087
  tipo: TipoTraslado;
  estado: EstadoTraslado;
  inicio: string;
  fin: string;
  rentaFolio: string;
  cliente: string;
  domicilio: Domicilio;
  equipos: number;
  firmaEnSitio: boolean;
}

// Equivale a EquipoTareaResponse
export interface EquipoTarea {
  id: number;
  numeroUnidad: string;
  fase: FaseEquipo;
  listo: boolean;
}

// Equivale a DetalleTareaResponse
export interface DetalleTarea extends Omit<Tarea, "equipos"> {
  estadoRenta: EstadoRenta;
  equipos: EquipoTarea[];
  listos: number;
  puedeIniciar: boolean;
  puedeCompletar: boolean;
  bloqueo: string | null;
}

export const NOMBRE_TIPO: Record<TipoTraslado, string> = {
  ENTREGA: "Entrega",
  RECOLECCION: "Recolección",
};

export const NOMBRE_ESTADO_RENTA: Record<EstadoRenta, string> = {
  PROGRAMADA: "Programada",
  EN_CURSO: "En curso",
  CERRADA: "Cerrada",
  CANCELADA: "Cancelada",
};

// Cómo se le dice a cada fase en la app del operador
export const NOMBRE_FASE: Record<FaseEquipo, string> = {
  RESERVADO: "Por preparar",
  EN_REVISION_SALIDA: "En revisión",
  LISTO_ENTREGA: "Listo para entrega",
  EN_TRASLADO: "En traslado",
  EN_OPERACION: "Con el cliente",
  EN_REVISION_ENTRADA: "En revisión de entrada",
  COMPLETADO: "Devuelto",
  CANCELADO: "Cancelado",
};