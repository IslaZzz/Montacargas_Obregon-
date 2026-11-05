import { getApi, patchApi } from "../../services/apiClient";
import type {
  Cotizacion,
  CotizacionResumen,
  EstadoCotizacion,
} from "./cotizacionesTypes";

// GET /api/cotizaciones, devuelve todas, la más nueva primero
export const listarCotizaciones = () =>
  getApi<CotizacionResumen[]>("/cotizaciones");

// GET /api/cotizaciones/5, devuelve el detalle
export const obtenerCotizacion = (id: number | string) =>
  getApi<Cotizacion>(`/cotizaciones/${id}`);

// GET /api/cotizaciones/5/pdf: dirección del PDF. Con descargar=true el navegador lo descarga en vez de abrirlo
export const urlPdf = (id: number, descargar = false) =>
  `/api/cotizaciones/${id}/pdf${descargar ? "?descargar=true" : ""}`;

// Trae el PDF como archivo, para compartirlo desde el celular
export async function obtenerPdf(id: number, nombre: string): Promise<File> {
  const respuesta = await fetch(urlPdf(id));
  if (!respuesta.ok) throw new Error(`Error ${respuesta.status}`);
  return new File([await respuesta.blob()], nombre, { type: "application/pdf" });
}

// PATCH /api/cotizaciones/5/estado, responde 409 si el cambio no se permite
export const cambiarEstadoCotizacion = (id: number, estado: EstadoCotizacion) =>
  patchApi<Cotizacion>(`/cotizaciones/${id}/estado`, { estado });
