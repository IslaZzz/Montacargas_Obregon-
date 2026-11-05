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

// PATCH /api/cotizaciones/5/estado, responde 409 si el cambio no se permite
export const cambiarEstadoCotizacion = (id: number, estado: EstadoCotizacion) =>
  patchApi<Cotizacion>(`/cotizaciones/${id}/estado`, { estado });
