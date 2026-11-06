import type { ChipStatus } from "../../components/chips";
import type {
  EstadoRevision,
  ResultadoRevision,
  Revision,
  RevisionesRenta,
  TipoRevision,
} from "./revisionesTypes";

// Estados en el orden de los filtros, con su texto y su chip
export const ESTADOS: {
  valor: EstadoRevision;
  etiqueta: string;
  chip: ChipStatus;
}[] = [
  { valor: "PENDIENTE", etiqueta: "Sin asignar", chip: "sin-asignar" },
  { valor: "ASIGNADO", etiqueta: "Asignada", chip: "asignado" },
  { valor: "EN_PROCESO", etiqueta: "En progreso", chip: "en-proceso" },
  { valor: "COMPLETADA", etiqueta: "Finalizada", chip: "finalizado" },
];

export const chipDeEstado = (estado: EstadoRevision): ChipStatus =>
  estado === "CANCELADA"
    ? "cancelado"
    : ESTADOS.find((e) => e.valor === estado)!.chip;

export const etiquetaDeEstado = (estado: EstadoRevision) =>
  estado === "CANCELADA"
    ? "Cancelada"
    : ESTADOS.find((e) => e.valor === estado)!.etiqueta;

// En pantalla, ENTRADA se llama "Regreso", como en la aplicación del mecánico
export const TIPOS: {
  valor: TipoRevision;
  etiqueta: string;
  chip: ChipStatus;
}[] = [
  { valor: "SALIDA", etiqueta: "Salida", chip: "salida" },
  { valor: "ENTRADA", etiqueta: "Regreso", chip: "regreso" },
];

export const chipDeTipo = (tipo: TipoRevision): ChipStatus =>
  TIPOS.find((t) => t.valor === tipo)!.chip;

export const chipDeResultado: Record<ResultadoRevision, ChipStatus> = {
  SIN_NOVEDAD: "sin-novedad",
  CON_OBSERVACIONES: "con-observaciones",
  REQUIERE_MANTENIMIENTO: "requiere-mantenimiento",
};

// Se pueden asignar o cambiar mientras no estén terminadas ni canceladas
export const esAsignable = (r: Revision) =>
  r.estado === "PENDIENTE" || r.estado === "ASIGNADO" || r.estado === "EN_PROCESO";

// Nombres distintos de los mecánicos que tiene una renta
export const mecanicosDelGrupo = (g: RevisionesRenta) => [
  ...new Set(g.revisiones.flatMap((r) => (r.mecanicoNombre ? [r.mecanicoNombre] : []))),
];

// El mecánico que comparten todas; null si son varios o ninguno
export function mecanicoComun(revisiones: Revision[]): number | null {
  const ids = new Set(revisiones.map((r) => r.mecanicoId));
  if (ids.size !== 1) return null;
  const [id] = ids;
  return id ?? null;
}