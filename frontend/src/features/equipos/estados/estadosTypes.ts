// Consulta de equipos por estado

// Equivale a EstadoConsultaEquipo
export type EstadoConsultaEquipo =
  | "DISPONIBLE"
  | "EN_PREPARACION"
  | "EN_RENTA"
  | "EN_MANTENIMIENTO"
  | "FUERA_SERVICIO";

// Equivale a EquipoEstadoResponse
export interface EquipoEstado {
  id: number;
  numeroUnidad: string;
  equipo: string; // "Toyota 8FGU25"
  categoria: string;
  estado: EstadoConsultaEquipo;
  rentaId: number | null; // Null si no está en una renta
  rentaFolio: string | null;
}