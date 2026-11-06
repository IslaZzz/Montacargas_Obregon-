export type TipoRevision = "SALIDA" | "ENTRADA";

export type EstadoRevision =
  | "PENDIENTE"
  | "ASIGNADO"
  | "EN_PROCESO"
  | "COMPLETADA"
  | "CANCELADA";

export type ResultadoRevision =
  | "SIN_NOVEDAD"
  | "CON_OBSERVACIONES"
  | "REQUIERE_MANTENIMIENTO";

// Equivale a RevisionResponse: la revisión de un equipo
export interface Revision {
  id: number;
  numeroUnidad: string;
  descripcionEquipo: string;
  fase: string;
  mecanicoId: number | null;
  mecanicoNombre: string | null;
  estado: EstadoRevision;
  resultado: ResultadoRevision | null;
  notas: string | null;
  fechaInicio: string | null;
  fechaFin: string | null;
}

// Equivale a RevisionesRentaResponse: una fila por renta y tipo
export interface RevisionesRenta {
  clave: string;
  rentaId: number;
  folioRenta: string;
  cliente: string;
  tipo: TipoRevision;
  fechaProgramada: string;
  estado: EstadoRevision;
  revisadas: number;
  total: number;
  revisiones: Revision[];
}

// Equivale a MecanicoCargaResponse
export interface MecanicoCarga {
  id: number;
  codigo: string;
  nombre: string;
  equiposActivos: number;
}

// Equivale a AsignarRevisionesRequest
export interface AsignarRevisionesRequest {
  revisionIds: number[];
  personalId: number;
}