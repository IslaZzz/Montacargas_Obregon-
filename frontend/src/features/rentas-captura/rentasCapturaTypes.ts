// Captura manual de rentas: temporal, mientras comercial hace su pantalla de rentas

// Equivale a EquipoDisponibleResponse
export interface EquipoDisponible {
  id: number;
  numeroUnidad: string;
  categoria: string; // "Montacargas"
  descripcion: string; // "Toyota 8FGU25"
  tarifaDia: number;
}

// Equivale a CrearRentaRequest. El horario de entrega es opcional
export interface CrearRentaRequest {
  clienteId: number;
  fechaInicio: string; // "2026-11-10"
  fechaFin: string;
  direccionObra: string;
  contactoObra: string;
  telefonoContacto: string;
  equipoIds: number[];
  entregaDesde: string | null; // "2026-11-10T08:00:00"
  entregaHasta: string | null;
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
  estado: string;
  equipos: string[]; // Números de unidad
}