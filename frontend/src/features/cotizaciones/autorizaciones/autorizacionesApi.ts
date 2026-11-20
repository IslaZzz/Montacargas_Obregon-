import { getApi, patchApi } from "../../../services/apiClient";
import type { AutorizacionResumen } from "../../cotizaciones/cotizacionesTypes";

// Evento que avisa al menú lateral que el contador de pendientes cambió
export const EVENTO_AUTORIZACIONES = "autorizaciones-cambiaron";

function avisarCambio() {
  window.dispatchEvent(new Event(EVENTO_AUTORIZACIONES));
}

// Siempre se piden todas; la pantalla filtra las pendientes
export function listarAutorizaciones() {
  return getApi<AutorizacionResumen[]>("/autorizaciones?soloPendientes=false");
}
export function contarAutorizacionesPendientes() {
  return getApi<number>("/autorizaciones/pendientes/total");
}

export async function autorizarDescuento(cotizacionId: number, comentario?: string) {
  const respuesta = await patchApi<AutorizacionResumen>(`/autorizaciones/${cotizacionId}/autorizar`, {
    comentario: comentario?.trim() || null,
  });
  avisarCambio();
  return respuesta;
}

export async function rechazarDescuento(cotizacionId: number, comentario: string) {
  const respuesta = await patchApi<AutorizacionResumen>(`/autorizaciones/${cotizacionId}/rechazar`, {
    comentario: comentario.trim(),
  });
  avisarCambio();
  return respuesta;
}