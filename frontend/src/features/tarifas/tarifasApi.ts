import { getApi, patchApi } from "../../services/apiClient"
import type { Tarifa, TarifaRequest } from './tarifasTypes'

// GET /api/tarifas, devuelve la tarifa por día de cada modelo
export const listarTarifas = () => getApi<Tarifa[]>('/tarifas')

// PATCH /api/tarifas/{modeloId}, cambia la tarifa por día de un modelo
export const actualizarTarifa = (modeloId: number, solicitud: TarifaRequest) =>
  patchApi<Tarifa>(`/tarifas/${modeloId}`, solicitud)