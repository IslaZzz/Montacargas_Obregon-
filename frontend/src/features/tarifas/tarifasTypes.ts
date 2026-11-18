// Equivale a TarifaResponse
export interface Tarifa {
  modeloId: number
  nombre: string // Ej. "Montacargas eléctrico 3 t"
  marca: string // Ej. "Toyota"
  codigo: string // Ej. "8FBE30"
  categoriaId: number
  categoria: string // Ej. "Montacargas"
  tarifaDia: number | null // Pesos por día; null si el modelo aún no tiene tarifa (HU-36)
}

// Equivale a TarifaRequest
export interface TarifaRequest {
  tarifaDia: number
}