import type { ChipStatus } from "../../components/chips";
import type {
  EstadoMantenimiento,
  Mantenimiento,
} from "./mantenimientosTypes";

// Estados en el orden de los filtros, con su texto y su chip
export const ESTADOS: {
  valor: EstadoMantenimiento;
  etiqueta: string;
  chip: ChipStatus;
}[] = [
  { valor: "PENDIENTE", etiqueta: "Sin asignar", chip: "sin-asignar" },
  { valor: "ASIGNADO", etiqueta: "Asignado", chip: "asignado" },
  { valor: "EN_PROCESO", etiqueta: "En progreso", chip: "en-proceso" },
  { valor: "COMPLETADO", etiqueta: "Finalizado", chip: "finalizado" },
];

export const chipDeEstado = (estado: EstadoMantenimiento): ChipStatus =>
  estado === "CANCELADO"
    ? "cancelado"
    : ESTADOS.find((e) => e.valor === estado)!.chip;

// Pendientes primero; dentro de cada estado, el que sale antes
const ORDEN: Record<EstadoMantenimiento, number> = {
  PENDIENTE: 0,
  ASIGNADO: 1,
  EN_PROCESO: 2,
  COMPLETADO: 3,
  CANCELADO: 4,
};

export const ordenar = (lista: Mantenimiento[]) =>
  [...lista].sort(
    (a, b) =>
      ORDEN[a.estado] - ORDEN[b.estado] ||
      (a.fechaLimite ?? "9999").localeCompare(b.fechaLimite ?? "9999"),
  );