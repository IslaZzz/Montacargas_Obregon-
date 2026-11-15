// Equivale a TarifaResponse
export interface Tarifa {
  modeloId: number
  nombre: string // Ej. "Montacargas eléctrico 3 t"
  marca: string // Ej. "Toyota"
  codigo: string // Ej. "8FBE30"
  categoriaId: number
  categoria: string // Ej. "Montacargas"
  tarifaDia: number // Pesos por día
}

// Equivale a TarifaRequest
export interface TarifaRequest {
  tarifaDia: number
}