import type { FiltroTraslados, Traslado } from "./trasladosTypes";

const DIAS_CORTOS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const DIAS_LARGOS = [
  "domingo",
  "lunes",
  "martes",
  "miércoles",
  "jueves",
  "viernes",
  "sábado",
];
const MESES_CORTOS = [
  "ene",
  "feb",
  "mar",
  "abr",
  "may",
  "jun",
  "jul",
  "ago",
  "sep",
  "oct",
  "nov",
  "dic",
];
const MESES_LARGOS = [
  "enero",
  "febrero",
  "marzo",
  "abril",
  "mayo",
  "junio",
  "julio",
  "agosto",
  "septiembre",
  "octubre",
  "noviembre",
  "diciembre",
];

/**
 * Parsea una cadena ISO sin desplazamiento de zona horaria indeseado.
 */
export function parsearFechaLocal(iso: string): Date {
  if (!iso) return new Date();
  if (iso.includes("T")) {
    const [fechaParte, tiempoParte] = iso.split("T");
    const [y, m, d] = fechaParte.split("-").map(Number);
    const tiempoLimpio = tiempoParte.replace("Z", "");
    const [h, min] = tiempoLimpio.slice(0, 5).split(":").map(Number);
    return new Date(y, m - 1, d, h || 0, min || 0);
  }
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d);
}

const pad = (n: number) => String(n).padStart(2, "0");

/**
 * Ej: "Mié 07 oct"
 */
export function formatoDiaSemanaFecha(iso: string): string {
  const d = parsearFechaLocal(iso);
  const diaSemana = DIAS_CORTOS[d.getDay()];
  const dia = pad(d.getDate());
  const mes = MESES_CORTOS[d.getMonth()];
  return `${diaSemana} ${dia} ${mes}`;
}

/**
 * Ej: "08:00 – 09:30"
 */
export function formatoRangoHorario(inicioIso: string, finIso: string): string {
  const dInicio = parsearFechaLocal(inicioIso);
  const dFin = parsearFechaLocal(finIso);

  const horaInicio = `${pad(dInicio.getHours())}:${pad(dInicio.getMinutes())}`;
  const horaFin = `${pad(dFin.getHours())}:${pad(dFin.getMinutes())}`;
  return `${horaInicio} – ${horaFin}`;
}

/**
 * Ej: "Mié 07 oct · 15:00 – 17:00"
 */
export function formatoFechaHorarioResumen(inicioIso: string, finIso: string): string {
  return `${formatoDiaSemanaFecha(inicioIso)} · ${formatoRangoHorario(inicioIso, finIso)}`;
}

/**
 * Ej: "miércoles 07 de octubre, 15:00 – 17:00"
 */
export function formatoDisponibilidadSubtitulo(inicioIso: string, finIso: string): string {
  const dInicio = parsearFechaLocal(inicioIso);
  const dFin = parsearFechaLocal(finIso);

  const diaSemana = DIAS_LARGOS[dInicio.getDay()];
  const dia = pad(dInicio.getDate());
  const mes = MESES_LARGOS[dInicio.getMonth()];
  const horario = `${pad(dInicio.getHours())}:${pad(dInicio.getMinutes())} – ${pad(dFin.getHours())}:${pad(dFin.getMinutes())}`;

  return `Disponibilidad para el ${diaSemana} ${dia} de ${mes}, ${horario}.`;
}

/**
 * Configuración de los 4 chips de filtro superior
 */
export interface OpcionFiltroChip {
  id: FiltroTraslados;
  label: string;
  dotColor?: string;
}

export const CHIPS_FILTRO: OpcionFiltroChip[] = [
  { id: "TODOS", label: "Todos" },
  { id: "SIN_ASIGNAR", label: "Sin asignar", dotColor: "#BA3636" },
  { id: "ENTREGAS", label: "Entregas", dotColor: "#209568" },
  { id: "RECOLECCIONES", label: "Recolecciones", dotColor: "#D58A11" },
];

/**
 * Cuenta los elementos según el filtro activo
 */
export function contarPorFiltro(traslados: Traslado[], filtro: FiltroTraslados): number {
  switch (filtro) {
    case "TODOS":
      return traslados.length;
    case "SIN_ASIGNAR":
      return traslados.filter((t) => t.responsableId === null).length;
    case "ENTREGAS":
      return traslados.filter((t) => t.tipo === "ENTREGA").length;
    case "RECOLECCIONES":
      return traslados.filter((t) => t.tipo === "RECOLECCION").length;
  }
}

