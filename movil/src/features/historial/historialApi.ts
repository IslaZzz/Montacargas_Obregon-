import { getApi } from "../../services/api";
import type { ResultadoRevision, TipoRevision } from "../revisiones/revisionesTypes";

// Equivale a HistorialMecanicoResponse
export interface Trabajo {
  trabajo: "REVISION" | "MANTENIMIENTO";
  id: number;
  unidad: string;
  tipoEquipo: string | null;
  terminado: string;
  tipoRevision: TipoRevision | null; // Solo revisiones
  resultado: ResultadoRevision | null; // Solo revisiones
  tareas: number | null; // Solo mantenimientos
}

// GET /api/movil/historial: lo que terminé en los últimos 30 días
export const listarHistorial = () => getApi<Trabajo[]>("/movil/historial");