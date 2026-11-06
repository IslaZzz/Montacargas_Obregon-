export type EstadoMantenimiento =
  | "PENDIENTE"
  | "ASIGNADO"
  | "EN_PROCESO"
  | "COMPLETADO"
  | "CANCELADO";

// Equivale a MantenimientoResponse
export interface Mantenimiento {
  id: number;
  numeroUnidad: string;
  descripcionEquipo: string;
  tipo: "PREVENTIVO" | "CORRECTIVO";
  descripcionFalla: string;
  reportadoPor: string | null;
  fechaReporte: string;
  fechaLimite: string | null;
  folioRenta: string | null; // Siempre null hasta que exista Renta
  mecanicoId: number | null;
  mecanicoNombre: string | null;
  estado: EstadoMantenimiento;
}

// Equivale a AsignarRequest
export interface AsignarRequest {
  mantenimientoIds: number[];
  personalId: number;
}