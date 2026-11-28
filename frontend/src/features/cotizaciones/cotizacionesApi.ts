import { getApi, patchApi, putApi } from "../../services/apiClient";
import type {
  CorregirCotizacionRequest,
  Cotizacion,
  CotizacionResumen,
  DescuentoRequest,
  EstadoCotizacion,
  TrasladoRequest,
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

// PATCH /api/cotizaciones/5/traslado: guarda el costo de traslado de una cotización en Borrador.
// Responde 409 si la cotización ya no es Borrador y 400 si algún monto es negativo
export const guardarTraslado = (
  id: number,
  traslado: TrasladoRequest,
) => patchApi<Cotizacion>(`/cotizaciones/${id}/traslado`, traslado);

// PATCH /api/cotizaciones/5/descuento: aplica o quita el descuento (HU-15).
// Con descuento la cotización pasa a En autorización. Responde 400 si no es válido y 409 si ya no es Borrador
export const guardarDescuento = (id: number, descuento: DescuentoRequest) =>
  patchApi<Cotizacion>(`/cotizaciones/${id}/descuento`, descuento);

// PUT /api/cotizaciones/5/correccion: guarda una cotización rechazada ya corregida (HU-32).
// Con descuento vuelve a En autorización; sin descuento queda en Borrador.
// Responde 409 si ya no está Rechazada y 400 si algún dato o el descuento no son válidos
export const corregirCotizacion = (id: number, datos: CorregirCotizacionRequest) =>
  putApi<Cotizacion>(`/cotizaciones/${id}/correccion`, datos);

// Marca el precio como definido al terminar el último paso
export const definirPrecioCotizacion = (id: number) =>
  patchApi<Cotizacion>(`/cotizaciones/${id}/precio-definido`, {});