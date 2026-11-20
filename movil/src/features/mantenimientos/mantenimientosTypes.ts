import type { TonoChip } from "../../components/Chip";
import type { Especificacion } from "../../components/Especificaciones";
import type { ValorRevision } from "../revisiones/revisionesTypes";

// Equivalen a los enums del backend
export type EstadoMantenimiento = "PENDIENTE" | "ASIGNADO" | "EN_PROCESO" | "COMPLETADO" | "CANCELADO";
export type TipoMantenimiento = "CORRECTIVO" | "PREVENTIVO";
export type EstadoTarea = "PENDIENTE" | "ATENDIDA";
export type OrigenTarea = "ASIGNADA" | "AGREGADA";

// Equivale a MantenimientoMecanicoResponse
export interface MantenimientoMecanico {
  id: number;
  unidad: string;
  tipoEquipo: string | null;
  tipo: TipoMantenimiento;
  estado: EstadoMantenimiento;
  fechaLimite: string | null;
}

// Equivale a TareaMecanicoResponse
export interface Tarea {
  id: number;
  titulo: string;
  hallazgo: string | null;
  origen: OrigenTarea;
  componente: string | null;
  accionSugerida: string | null;
  estado: EstadoTarea;
  trabajoRealizado: string | null;
  refacciones: string | null;
  tiempoInvertido: string | null;
  observaciones: string | null;
  atendidaEn: string | null;
  incidencia: string | null;
}

// Equivale a DetalleMantenimientoMecanicoResponse
export interface DetalleMantenimiento extends MantenimientoMecanico {
  modelo: string | null;
  especificaciones: Especificacion[];
  descripcionFalla: string;
  fechaFin: string | null;
  revision: ValorRevision[]; // Lo que se anotó en la revisión de la que salió (sin fotos)
  tareas: Tarea[];
  atendidas: number;
  puedeFinalizar: boolean;
  fotosPorTarea: string[];
}

// Lo que se manda al atender una tarea (AtenderTareaRequest)
export interface Atencion {
  trabajoRealizado: string;
  refacciones: string | null;
  tiempoInvertido: string | null;
  observaciones: string | null;
}

// "Nuevo" es asignado y aún no lo abre; "En curso", ya lo abrió
export const NOMBRE_ESTADO: Partial<Record<EstadoMantenimiento, string>> = {
  ASIGNADO: "Nuevo",
  EN_PROCESO: "En curso",
  COMPLETADO: "Terminado",
};

export const TONO_ESTADO: Partial<Record<EstadoMantenimiento, TonoChip>> = {
  ASIGNADO: "amarillo",
  EN_PROCESO: "azul",
  COMPLETADO: "verde",
};

/** Cómo se muestra el estado de una tarea. */
export function estadoDeTarea(tarea: Tarea): { texto: string; tono: TonoChip } {
  if (tarea.estado === "ATENDIDA") return { texto: "Atendida", tono: "verde" };
  if (tarea.incidencia) return { texto: "Con incidencia", tono: "rojo" };
  return { texto: "Pendiente", tono: "amarillo" };
}

/** "Tarea 2 de 3" */
export function posicionDeTarea(detalle: DetalleMantenimiento, tarea: Tarea) {
  return `Tarea ${detalle.tareas.findIndex((t) => t.id === tarea.id) + 1} de ${detalle.tareas.length}`;
}