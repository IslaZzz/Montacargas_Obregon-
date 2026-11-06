import { getApi } from "../../services/apiClient";
import type { Inicio } from "./dashboardTypes";

// GET /api/inicio: pendientes de hoy, avisos y próximas operaciones
export const obtenerInicio = () => getApi<Inicio>("/inicio");