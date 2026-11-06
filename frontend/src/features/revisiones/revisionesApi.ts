import { getApi, patchApi } from "../../services/apiClient";
import type {
  AsignarRevisionesRequest,
  MecanicoCarga,
  RevisionesRenta,
} from "./revisionesTypes";

// GET /api/revisiones, una fila por renta y tipo
export const listarRevisiones = () => getApi<RevisionesRenta[]>("/revisiones");

// GET /api/revisiones/mecanicos, mecánicos activos con su carga
export const listarMecanicosRevision = () =>
  getApi<MecanicoCarga[]>("/revisiones/mecanicos");

// PATCH /api/revisiones/asignar, devuelve la lista completa ya actualizada
export const asignarRevisiones = (solicitud: AsignarRevisionesRequest) =>
  patchApi<RevisionesRenta[]>("/revisiones/asignar", solicitud);