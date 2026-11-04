import { getApi } from '../../services/apiClient'
import type { Cliente } from './clientesTypes'

// GET /api/clientes?texto=..., devuelve una lista de Cliente
export const buscarClientes = (texto = '') =>
  getApi<Cliente[]>(`/clientes?texto=${encodeURIComponent(texto)}`)