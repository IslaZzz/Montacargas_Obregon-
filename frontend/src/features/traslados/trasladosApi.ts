import { getApi, patchApi, postApi } from "../../services/apiClient";
import type {
  OperadorDisponibilidad,
  Traslado,
} from "./trasladosTypes";

/**
 * Consulta la lista de traslados con filtros opcionales.
 */
export async function listarTraslados(
  filtro?: string,
  fechaInicio?: string,
  fechaFin?: string
): Promise<Traslado[]> {
  const params = new URLSearchParams();
  if (filtro && filtro !== "TODOS") params.set("filtro", filtro);
  if (fechaInicio) params.set("fechaInicio", fechaInicio);
  if (fechaFin) params.set("fechaFin", fechaFin);

  const query = params.toString() ? `?${params.toString()}` : "";
  return getApi<Traslado[]>(`/traslados${query}`);
}

/**
 * Consulta los operadores y su disponibilidad para un traslado o ventana de horario.
 */
export async function consultarDisponibilidad(
  trasladoId?: number,
  fechaHoraInicio?: string,
  fechaHoraFin?: string
): Promise<OperadorDisponibilidad[]> {
  const params = new URLSearchParams();
  if (trasladoId) params.set("trasladoId", String(trasladoId));
  if (fechaHoraInicio) params.set("fechaHoraInicio", fechaHoraInicio);
  if (fechaHoraFin) params.set("fechaHoraFin", fechaHoraFin);

  const query = params.toString() ? `?${params.toString()}` : "";
  return getApi<OperadorDisponibilidad[]>(`/traslados/disponibilidad${query}`);
}

/**
 * Asigna o cambia el responsable de un traslado.
 * Rechazará con ErrorApi si existe empalme de horarios o si ya inició.
 */
export async function asignarResponsable(
  trasladoId: number,
  personalId: number
): Promise<Traslado> {
  return postApi<Traslado>(`/traslados/${trasladoId}/asignar`, {
    personalId,
  });
}

/**
 * Marca el traslado como realizado, cuando el operador confirma que ya entregó o recogió.
 * Rechazará con ErrorApi si no tiene responsable o ya estaba realizado.
 */
export async function marcarRealizado(trasladoId: number): Promise<Traslado> {
  return patchApi<Traslado>(`/traslados/${trasladoId}/completar`, {});
}

