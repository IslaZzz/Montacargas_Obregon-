// Confirmación temporal del horario de recolección (la renta ya nace de la cotización, HU-37)

// Equivale a HorarioRequest
export interface HorarioRequest {
  desde: string; // "2026-11-20T14:00:00"
  hasta: string;
}

// Equivale a RentaCapturadaResponse
export interface RentaCreada {
  id: number;
  folio: string;
  cliente: string;
  fechaInicio: string;
  fechaFin: string;
  entregaDesde: string | null;
  entregaHasta: string | null;
  recoleccionDesde: string | null;
  recoleccionHasta: string | null;
  estado: string;
  equipos: string[]; // Números de unidad
}