import { getApi } from "../../services/apiClient";
import type { Operacion, RevisionesRenta } from "./dashboardTypes";
import { contarPreparaciones } from "./dashboardUtils";
import { operacionesDeEjemplo } from "./operacionesEjemplo";

// GET /api/revisiones: una fila por renta y tipo (salida o regreso)
export const listarRevisiones = () => getApi<RevisionesRenta[]>("/revisiones");

// Las revisiones de salida son las preparaciones de los equipos
export const contarPreparacionesDeHoy = async (hoy: Date) =>
  contarPreparaciones(await listarRevisiones(), hoy);

// Entregas y recolecciones.
// TODO: conectar al backend cuando existan los traslados; mientras tanto regresa datos de ejemplo.
export const listarOperaciones = async (hoy: Date): Promise<Operacion[]> =>
  operacionesDeEjemplo(hoy);
