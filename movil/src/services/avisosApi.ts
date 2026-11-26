import { getApi, patchApi } from "./api";

// Equivale a AvisoOperacionResponse
export interface AvisoMovil {
  id: number;
  tipo: "TAREA_REASIGNADA";
  titulo: string; // "La tarea TR-0087 se reasignó"
  detalle: string;
  rentaId: number | null;
  fecha: string;
  leido: boolean;
}

// GET /api/movil/avisos: los que todavía no ve
export const listarAvisos = () => getApi<AvisoMovil[]>("/movil/avisos");

// PATCH /api/movil/avisos/leidos
export const marcarAvisosLeidos = () => patchApi<void>("/movil/avisos/leidos", {});