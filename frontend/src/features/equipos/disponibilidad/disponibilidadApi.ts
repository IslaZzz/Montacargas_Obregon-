import { getApi } from '../../../services/apiClient'

export type Etiqueta = 'APTO_URGENCIAS' | 'REQUIERE_OPERADOR' | 'USO_INTERIORES'

export interface Categoria {
  id: number
  nombre: string
}

export interface EquipoDisponible {
  id: number
  numeroUnidad: string
  modelo: string
  etiquetas: Etiqueta[]
  tarifaDia: number
}

export interface Disponibilidad {
  equipos: EquipoDisponible[]
  ocupados: number
}

export const listarCategorias = () => getApi<Categoria[]>('/categorias')

export const consultarDisponibilidad = (
  categoriaId: number,
  etiqueta: Etiqueta | '',
  desde: string,
  hasta: string
) => {
  const parametros = new URLSearchParams({ categoriaId: String(categoriaId), desde, hasta })
  if (etiqueta) parametros.set('etiqueta', etiqueta)
  return getApi<Disponibilidad>(`/disponibilidad?${parametros}`)
}