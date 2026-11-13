// Equivale a TipoPersonal del backend
export type TipoPersonal = "MECANICO" | "OP_TRASLADO";

// Equivale a PersonalActual: quién usa la app
export interface Personal {
  id: number;
  codigo: string; // "MEC-10"
  nombre: string; // "Rafael Amaya"
  tipo: TipoPersonal;
}

export const NOMBRE_TIPO: Record<TipoPersonal, string> = {
  MECANICO: "Mecánico",
  OP_TRASLADO: "Operador de traslado",
};