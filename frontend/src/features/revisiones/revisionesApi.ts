import { getApi, patchApi } from "../../services/apiClient";
import type {
  AsignarRevisionesRequest,
  RegistrarResultadoRequest,
  RevisionesRenta,
} from "./revisionesTypes";

// GET /api/revisiones, una fila por renta y tipo
export const listarRevisiones = () => getApi<RevisionesRenta[]>("/revisiones");

// PATCH /api/revisiones/asignar, devuelve la lista completa ya actualizada
export const asignarRevisiones = (solicitud: AsignarRevisionesRequest) =>
  patchApi<RevisionesRenta[]>("/revisiones/asignar", solicitud);

// PATCH /api/revisiones/{id}/resultado, devuelve la lista completa ya actualizada
export const registrarResultado = (id: number, solicitud: RegistrarResultadoRequest) =>
  patchApi<RevisionesRenta[]>(`/revisiones/${id}/resultado`, solicitud);