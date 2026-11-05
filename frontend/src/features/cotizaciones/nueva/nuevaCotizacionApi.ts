import { getApi, postApi } from "../../../services/apiClient";
import type { Cotizacion } from "../cotizacionesTypes";
import type { CrearCotizacionRequest, EquipoCatalogo } from "./nuevaCotizacionTypes";

export const crearCotizacion = (solicitud: CrearCotizacionRequest) =>
  postApi<Cotizacion>("/cotizaciones", solicitud);

export const listarEquiposOperativos = async () => {
  const equipos = await getApi<EquipoCatalogo[]>("/equipos");
  return equipos.filter((e) => e.estado === "OPERATIVO");
};
