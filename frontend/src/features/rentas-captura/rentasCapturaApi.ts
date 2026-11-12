import { getApi, patchApi } from "../../services/apiClient";
import type { HorarioRequest, RentaCreada } from "./rentasCapturaTypes";

// GET /api/rentas/sin-recoleccion, rentas en curso que aún no tienen horario de recolección
export const listarRentasSinRecoleccion = () =>
  getApi<RentaCreada[]>("/rentas/sin-recoleccion");

// PATCH /api/rentas/{id}/recoleccion, guarda el horario y crea el traslado de recolección
export const confirmarRecoleccion = (id: number, horario: HorarioRequest) =>
  patchApi<RentaCreada>(`/rentas/${id}/recoleccion`, horario);