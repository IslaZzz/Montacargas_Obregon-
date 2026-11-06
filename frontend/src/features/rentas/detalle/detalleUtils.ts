import { aFecha } from "../../../utils/formato";

const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];

// "2026-10-08" o "2026-10-08T09:00:00" → "Jueves 8 de octubre"
export function diaLargo(texto: string) {
  const dia = aFecha(texto).toLocaleDateString("es-MX", { weekday: "long", day: "numeric", month: "long" });
  const limpio = dia.replace(",", "");
  return limpio.charAt(0).toUpperCase() + limpio.slice(1);
}

// "2026-10-09" → "9 oct"
export const diaMes = (texto: string) => {
  const f = aFecha(texto);
  return `${f.getDate()} ${MESES[f.getMonth()]}`;
};

// Periodo de la renta: "23 sep – 9 oct 2026"
export const periodo = (inicio: string, fin: string) => `${diaMes(inicio)} – ${diaMes(fin)} ${aFecha(fin).getFullYear()}`;

// Días entre dos fechas, contando los dos extremos: del 23 sep al 9 oct son 17
export function diasDeRenta(inicio: string, fin: string) {
  return Math.round((aFecha(fin).getTime() - aFecha(inicio).getTime()) / 86_400_000) + 1;
}

// "En 2 días", "Mañana", "Hoy" o "Hace 3 días"
export function cuandoEs(texto: string) {
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const dia = aFecha(texto);
  dia.setHours(0, 0, 0, 0);
  const diferencia = Math.round((dia.getTime() - hoy.getTime()) / 86_400_000);
  if (diferencia === 0) return "Hoy";
  if (diferencia === 1) return "Mañana";
  if (diferencia > 1) return `En ${diferencia} días`;
  return diferencia === -1 ? "Ayer" : `Hace ${-diferencia} días`;
}

// "08:00" a partir de "2026-10-08T08:00:00"
export const horaDe = (texto: string) => texto.slice(11, 16);

// "2026-10-08" a partir de "2026-10-08T08:00:00"
export const diaDe = (texto: string) => texto.slice(0, 10);

// Fecha de hoy como la usa un <input type="date">
export function hoyTexto() {
  const hoy = new Date();
  const dos = (n: number) => String(n).padStart(2, "0");
  return `${hoy.getFullYear()}-${dos(hoy.getMonth() + 1)}-${dos(hoy.getDate())}`;
}