import { getApi, patchApi } from "../../services/apiClient";
import type {
  AsignarRequest,
  Mantenimiento,
  MecanicoCarga,
} from "./mantenimientosTypes";

// GET /api/mantenimientos, todos los mantenimientos
export const listarMantenimientos = () =>
  getApi<Mantenimiento[]>("/mantenimientos");

// GET /api/mantenimientos/mecanicos, mecánicos activos con su carga
export const listarMecanicos = () =>
  getApi<MecanicoCarga[]>("/mantenimientos/mecanicos");

// PATCH /api/mantenimientos/asignar, devuelve los mantenimientos ya actualizados
export const asignarMantenimientos = (solicitud: AsignarRequest) =>
  patchApi<Mantenimiento[]>("/mantenimientos/asignar", solicitud);