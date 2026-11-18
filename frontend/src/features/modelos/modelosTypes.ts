export type TipoCampo = "TEXTO" | "ENTERO" | "DECIMAL" | "BOOLEANO" | "FECHA" | "LISTA";

// Equivale a DefinicionCampoResponse: un campo que pide la categoría, como «Capacidad de carga»
export interface CampoCategoria {
  id: number;
  nombre: string;
  tipo: TipoCampo;
  unidad: string | null; // Ej. "kg"
  requerido: boolean;
  orden: number;
  opciones: string[]; // Solo para LISTA
}

// Equivale a EspecificacionResponse
export interface Especificacion {
  campoId: number;
  nombre: string;
  tipo: TipoCampo;
  unidad: string | null;
  orden: number;
  valor: string; // Ej. "3000" o "Eléctrico"
}

// Equivale a ModeloDetalleResponse
export interface Modelo {
  id: number;
  nombre: string; // Ej. "Montacargas eléctrico 3 t"
  categoriaId: number;
  categoria: string;
  marca: string; // Ej. "Toyota"
  codigo: string; // Ej. "8FBE30"
  marcaModelo: string; // Ej. "Toyota 8FBE30"
  descripcion: string | null;
  tarifaDia: number | null; // null hasta que se captura en «Tarifas»
  especificaciones: Especificacion[];
}

// Equivale a ModeloRequest
export interface ModeloRequest {
  categoriaId: number | null;
  nombre: string;
  marcaModelo: string;
  descripcion: string | null;
  especificaciones: Record<number, string>; // Por id del campo
}
