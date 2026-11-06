import { getApi, patchApi } from "../../services/apiClient";
import type { AsignarRequest, Mantenimiento } from "./mantenimientosTypes";

// GET /api/mantenimientos, todos los mantenimientos
export const listarMantenimientos = () =>
  getApi<Mantenimiento[]>("/mantenimientos");

// PATCH /api/mantenimientos/asignar, devuelve los mantenimientos ya actualizados
export const asignarMantenimientos = (solicitud: AsignarRequest) =>
  patchApi<Mantenimiento[]>("/mantenimientos/asignar", solicitud);