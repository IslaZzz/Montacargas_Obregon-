// Fechas como las muestra la app. Si cambia un formato, se cambia solo aquí.
// El backend manda "2026-10-08" (día) o "2026-10-08T15:00:00" (día y hora), sin zona: son hora local.

const DIAS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
const dos = (n: number) => String(n).padStart(2, "0");

/** Texto del backend a Date local. "2026-10-08" se toma a las 00:00 de ese día, no en UTC. */
export function aFecha(texto: string): Date {
  return texto.length === 10 ? new Date(`${texto}T00:00:00`) : new Date(texto);
}

/** "2026-10-08", para agrupar o comparar días sin importar la hora. */
export const claveDia = (fecha: Date) => `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`;

/** Días de calendario de hoy a esa fecha: 0 hoy, 1 mañana, -1 ayer. */
export function diasDesdeHoy(fecha: Date, hoy = new Date()) {
  const a = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  const b = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate());
  return Math.round((b.getTime() - a.getTime()) / 86_400_000);
}

/** "15:00" */
export const hora = (texto: string) => {
  const f = aFecha(texto);
  return `${dos(f.getHours())}:${dos(f.getMinutes())}`;
};

/** "15:00 – 17:00" */
export const horario = (desde: string, hasta: string) => `${hora(desde)} – ${hora(hasta)}`;

/** "8 oct" */
export const diaMes = (texto: string) => {
  const f = aFecha(texto);
  return `${f.getDate()} ${MESES[f.getMonth()]}`;
};

/** Encabezado de un grupo de tareas: "Hoy · 8 oct", "Mañana · 9 oct", "Sáb · 10 oct" */
export function etiquetaDia(texto: string, hoy = new Date()) {
  const f = aFecha(texto);
  const dias = diasDesdeHoy(f, hoy);
  const nombre = dias === 0 ? "Hoy" : dias === 1 ? "Mañana" : dias === -1 ? "Ayer" : DIAS[f.getDay()];
  return `${nombre} · ${diaMes(texto)}`;
}

/** Fecha y hora corta para tarjetas: "Hoy · 15:00", "Vie 23 · 10:00" */
export function fechaHoraCorta(texto: string, hoy = new Date()) {
  const f = aFecha(texto);
  const dias = diasDesdeHoy(f, hoy);
  const dia = dias === 0 ? "Hoy" : dias === 1 ? "Mañana" : dias === -1 ? "Ayer" : `${DIAS[f.getDay()]} ${f.getDate()}`;
  return `${dia} · ${hora(texto)}`;
}

/** Agrupa por día en el orden en que vienen. Sirve para "Hoy · 8 oct" con sus tareas debajo. */
export function agruparPorDia<T>(elementos: T[], fechaDe: (e: T) => string) {
  const grupos = new Map<string, { dia: string; elementos: T[] }>();
  for (const e of elementos) {
    const clave = claveDia(aFecha(fechaDe(e)));
    if (!grupos.has(clave)) grupos.set(clave, { dia: fechaDe(e), elementos: [] });
    grupos.get(clave)!.elementos.push(e);
  }
  return [...grupos.values()];
}