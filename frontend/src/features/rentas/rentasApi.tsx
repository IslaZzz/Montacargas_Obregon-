import {getApi,putApi} from "../../services/apiClient";
import type {Renta} from "./estadosRentaTypes";

export interface RecoleccionRequest {
  desde: string;
  hasta: string;
}

export const listarRentas = () =>
  getApi<Renta[]>("/rentas");

export const actualizarRecoleccion = (
  id: number,
  datos: RecoleccionRequest
) =>
  putApi<Renta>(`/rentas/${id}/recoleccion`, datos);
