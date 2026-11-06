import { getApi, patchApi, postApi } from "../../services/apiClient";
import type {
  CrearRentaRequest,
  EquipoDisponible,
  HorarioRequest,
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

// GET /api/rentas/sin-recoleccion, rentas en curso que aún no tienen horario de recolección
export const listarRentasSinRecoleccion = () =>
  getApi<RentaCreada[]>("/rentas/sin-recoleccion");

// PATCH /api/rentas/{id}/recoleccion, guarda el horario y crea el traslado de recolección
export const confirmarRecoleccion = (id: number, horario: HorarioRequest) =>
  patchApi<RentaCreada>(`/rentas/${id}/recoleccion`, horario);