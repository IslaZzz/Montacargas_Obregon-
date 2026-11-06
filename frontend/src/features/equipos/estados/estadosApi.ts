import { getApi } from "../../../services/apiClient";
import type { EquipoEstado } from "./estadosTypes";

// GET /api/equipos/estados; con inicio y fin, el estado en ese periodo
export function consultarEquiposPorEstado(inicio?: string, fin?: string) {
  const parametros = new URLSearchParams();
  if (inicio && fin) {
    parametros.set("inicio", inicio);
    parametros.set("fin", fin);
  }
  const query = parametros.toString() ? `?${parametros}` : "";
  return getApi<EquipoEstado[]>(`/equipos/estados${query}`);
}