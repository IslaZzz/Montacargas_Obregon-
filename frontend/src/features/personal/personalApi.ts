import { getApi, postApi, putApi } from "../../services/apiClient";
import type { DatosPersonal, Personal } from "./personalTypes";

// Todos, activos y de baja (GET /api/personal sin filtros)
export const listarPersonal = () => getApi<Personal[]>("/personal");

export const obtenerPersonal = (id: number | string) => getApi<Personal>(`/personal/${id}`);

// Si el código ya existe o un dato no es válido, lanza ErrorApi con el motivo
export const registrarPersonal = (datos: DatosPersonal) => postApi<Personal>("/personal", datos);

export const actualizarPersonal = (id: number, datos: DatosPersonal) =>
  putApi<Personal>(`/personal/${id}`, datos);
