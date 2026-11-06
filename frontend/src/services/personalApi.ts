import { getApi } from "./apiClient";

// Equivale a MecanicoCargaResponse
export interface MecanicoCarga {
  id: number;
  codigo: string;
  nombre: string;
  equiposActivos: number; // Revisiones y mantenimientos sin terminar
}

// GET /api/personal/mecanicos, mecánicos activos con su carga de trabajo
export const listarMecanicos = () =>
  getApi<MecanicoCarga[]>("/personal/mecanicos");