import type { Foto } from "../../services/fotosApi";
import type { Domicilio } from "../../utils/domicilio";

// Equivalen a los enums del backend
export type TipoTraslado = "ENTREGA" | "RECOLECCION";
export type EstadoTraslado = "PROGRAMADO" | "EN_CURSO" | "COMPLETADO" | "CANCELADO";
export type EstadoRenta = "PROGRAMADA" | "EN_CURSO" | "CERRADA" | "CANCELADA";
// Equivale a CartaTarea: la carta responsiva en una entrega
export type CartaTarea = "SIN_GENERAR" | "PENDIENTE" | "FIRMADA";
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
  carta: CartaTarea | null; // null en las recolecciones
  fotosTomadas: number | null; // Evidencia de la recolección; null en las entregas
  fotosRequeridas: number | null;
  equipos: EquipoTarea[];
  listos: number;
  puedeIniciar: boolean;
  puedeCompletar: boolean;
  bloqueo: string | null;
}

// Equivale a EvidenciaResponse (OHU13)
export interface VistaEvidencia {
  vista: string; // "Frente", "Parte trasera", "Lateral izquierdo", "Lateral derecho"
  foto: Foto | null;
}

export interface EquipoEvidencia {
  id: number;
  numeroUnidad: string;
  tomadas: number;
  vistas: VistaEvidencia[];
}

export interface Evidencia {
  folio: string;
  rentaFolio: string;
  tomadas: number;
  requeridas: number;
  editable: boolean; // false una vez confirmada la recolección
  equipos: EquipoEvidencia[];
}

export const NOMBRE_TIPO: Record<TipoTraslado, string> = {
  ENTREGA: "Entrega",
  RECOLECCION: "Recolección",
};

export const NOMBRE_ESTADO_TRASLADO: Record<EstadoTraslado, string> = {
  PROGRAMADO: "Programado",
  EN_CURSO: "En camino",
  COMPLETADO: "Realizado",
  CANCELADO: "Cancelado",
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