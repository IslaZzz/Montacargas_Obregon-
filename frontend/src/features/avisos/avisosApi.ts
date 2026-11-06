import { getApi } from "../../services/apiClient";
import type { ApartadoProximo } from "./avisosTypes";

// Rentas que empiezan entre hoy y dentro de `dias` días y no tienen la carta firmada
export const listarApartadosProximos = (dias: number) =>
  getApi<ApartadoProximo[]>(`/avisos/apartados-proximos?dias=${dias}`);
