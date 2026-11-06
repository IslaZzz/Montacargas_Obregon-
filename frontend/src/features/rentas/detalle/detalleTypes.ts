// Detalle de una renta, como lo manda GET /api/rentas/{id}/detalle

export type EstadoRenta = "PROGRAMADA" | "EN_CURSO" | "CERRADA" | "CANCELADA";

export type FaseEquipoRenta =
  | "RESERVADO"
  | "EN_REVISION_SALIDA"
  | "LISTO_ENTREGA"
  | "EN_TRASLADO"
  | "EN_OPERACION"
  | "EN_REVISION_ENTRADA"
  | "COMPLETADO"
  | "CANCELADO";

export interface TrasladoRecoleccion {
  id: number;
  folio: string;
  estado: "PROGRAMADO" | "EN_CURSO" | "COMPLETADO" | "CANCELADO";
  yaInicio: boolean;
  responsable: string | null; // Null si no tiene operador
}

export interface EquipoDeRenta {
  id: number;
  numeroUnidad: string;
  equipo: string; // "Toyota 8FGU25"
  categoria: string;
  fase: FaseEquipoRenta;
}

export interface DetalleRenta {
  id: number;
  folio: string;
  estado: EstadoRenta;
  cliente: string;
  contacto: string;
  telefono: string;
  direccion: string;
  fechaInicio: string; // "2026-10-12"
  fechaFin: string;
  entregaDesde: string | null; // "2026-10-12T08:00:00"
  entregaHasta: string | null;
  recoleccionDesde: string | null;
  recoleccionHasta: string | null;
  trasladoRecoleccion: TrasladoRecoleccion | null; // Null mientras no se confirma
  equipos: EquipoDeRenta[];
}

// Equivale a HorarioRecoleccionRequest
export interface HorarioRecoleccion {
  desde: string; // "2026-10-09T09:00:00"
  hasta: string;
}