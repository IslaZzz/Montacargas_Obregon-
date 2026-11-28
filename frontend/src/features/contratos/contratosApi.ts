import { getApi } from "../../services/apiClient";
import type { Contrato } from "./contratosTypes";

// GET /api/contratos/5, devuelve el detalle
export const obtenerContrato = (id: number | string) =>
  getApi<Contrato>(`/contratos/${id}`);

// GET /api/contratos/5/pdf: dirección del PDF. Con descargar=true el navegador lo descarga en vez de abrirlo
export const urlPdfContrato = (id: number, descargar = false) =>
  `/api/contratos/${id}/pdf${descargar ? "?descargar=true" : ""}`;
