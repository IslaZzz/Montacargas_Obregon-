// Equivale a TipoPersonal del backend
export type TipoPersonal = "MECANICO" | "OP_TRASLADO";

// Equivale a PersonalActual: quién usa la app
export interface Personal {
  id: number;
  codigo: string; // "MEC-10"
  nombre: string; // "Rafael Amaya"
  tipo: TipoPersonal;
}

// Equivale a SesionMovilResponse (POST /api/movil/sesion). El token solo llega aquí.
export interface SesionIniciada {
  token: string;
  venceEn: string; // Instante UTC ISO-8601, por ejemplo "2026-11-20T03:00:00Z"
  personal: Personal;
}

// Equivale a SesionVigenteResponse (GET /api/movil/yo)
export interface SesionVigente {
  venceEn: string;
  personal: Personal;
}

export const NOMBRE_TIPO: Record<TipoPersonal, string> = {
  MECANICO: "Mecánico",
  OP_TRASLADO: "Operador de traslado",
};
