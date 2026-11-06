import { getApi, patchApi } from "../../../services/apiClient";
import type { DetalleRenta, HorarioRecoleccion } from "./detalleTypes";

// GET /api/rentas/{id}/detalle
export const obtenerDetalleRenta = (id: string) => getApi<DetalleRenta>(`/rentas/${id}/detalle`);

// PATCH /api/rentas/{id}/detalle/recoleccion, confirma o cambia el horario de recolección
export const confirmarRecoleccion = (id: number, horario: HorarioRecoleccion) =>
  patchApi<DetalleRenta>(`/rentas/${id}/detalle/recoleccion`, horario);