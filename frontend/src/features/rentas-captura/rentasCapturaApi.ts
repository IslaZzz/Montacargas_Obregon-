import { getApi, postApi } from "../../services/apiClient";
import type {
  CrearRentaRequest,
  EquipoDisponible,
  RentaCreada,
} from "./rentasCapturaTypes";

// GET /api/rentas/equipos-disponibles, equipos operativos libres en esas fechas
export const listarEquiposDisponibles = (inicio: string, fin: string) =>
  getApi<EquipoDisponible[]>(
    `/rentas/equipos-disponibles?inicio=${inicio}&fin=${fin}`,
  );

// POST /api/rentas, guarda la renta (responde 201)
export const crearRenta = (solicitud: CrearRentaRequest) =>
  postApi<RentaCreada>("/rentas", solicitud);