import { getApi } from '../../../services/apiClient'

// Equivale a EtiquetaResponse; gerencia puede agregar más al registrar un equipo (HU-02)
export interface Etiqueta {
  id: number
  nombre: string
}

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

export const listarEtiquetas = () => getApi<Etiqueta[]>('/etiquetas')

export const consultarDisponibilidad = (
  categoriaId: number,
  etiquetaId: number | null,
  desde: string,
  hasta: string
) => {
  const parametros = new URLSearchParams({ categoriaId: String(categoriaId), desde, hasta })
  if (etiquetaId !== null) parametros.set('etiquetaId', String(etiquetaId))
  return getApi<Disponibilidad>(`/disponibilidad?${parametros}`)
}