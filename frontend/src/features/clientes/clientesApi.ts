import { getApi, postApi } from '../../services/apiClient'
import type { Cliente, ClienteRequest } from './clientesTypes'

// GET /api/clientes?texto=..., devuelve una lista de Cliente
export const buscarClientes = (texto = '') =>
  getApi<Cliente[]>(`/clientes?texto=${encodeURIComponent(texto)}`)

// POST /api/clientes, registra un cliente con sus razones sociales (responde 201)
export const registrarCliente = (solicitud: ClienteRequest) =>
  postApi<Cliente>('/clientes', solicitud)