import { aFecha } from "./formato";

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