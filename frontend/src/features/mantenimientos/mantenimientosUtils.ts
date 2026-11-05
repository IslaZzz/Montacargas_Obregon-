import type { ChipStatus } from "../../components/chips";
import { aFecha } from "../../utils/formato";
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

const DIAS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
const dosDigitos = (n: number) => String(n).padStart(2, "0");

// "2026-10-22T08:00:00" → "Jue 22 oct"
export function formatoDiaCorto(texto: string) {
  const f = aFecha(texto);
  return `${DIAS[f.getDay()]} ${f.getDate()} ${MESES[f.getMonth()]}`;
}

// → "08:00"
export function formatoHora(texto: string) {
  const f = aFecha(texto);
  return `${dosDigitos(f.getHours())}:${dosDigitos(f.getMinutes())}`;
}

// → "hoy 09:12", "ayer 16:20" o "Jue 22 oct 09:12"
export function formatoRelativo(texto: string) {
  const dia = aFecha(texto);
  dia.setHours(0, 0, 0, 0);
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const diferencia = Math.round((hoy.getTime() - dia.getTime()) / 86_400_000);

  const hora = formatoHora(texto);
  if (diferencia === 0) return `hoy ${hora}`;
  if (diferencia === 1) return `ayer ${hora}`;
  return `${formatoDiaCorto(texto)} ${hora}`;
}

// "Rafael Amaya" → "RA"
export const iniciales = (nombre: string) =>
  nombre
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("");

// 0 → "Sin equipos por atender", 1 → "1 equipo por atender"
export const textoCarga = (n: number) =>
  n === 0
    ? "Sin equipos por atender"
    : `${n} ${n === 1 ? "equipo" : "equipos"} por atender`;