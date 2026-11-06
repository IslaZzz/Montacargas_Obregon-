import { aFecha } from "../../utils/formato";

const dosDigitos = (n: number) => String(n).padStart(2, "0");

// → "2026-10-08", para comparar días sin importar la hora
export const claveDia = (fecha: Date) =>
  `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`;

export const mismoDia = (a: Date, b: Date) => claveDia(a) === claveDia(b);

// Días de calendario entre dos fechas, sin contar la hora
export function diasEntre(desde: Date, hasta: Date) {
  const inicio = new Date(desde.getFullYear(), desde.getMonth(), desde.getDate());
  const fin = new Date(hasta.getFullYear(), hasta.getMonth(), hasta.getDate());
  return Math.round((fin.getTime() - inicio.getTime()) / 86_400_000);
}

// Cómo se nombra el día en "Próximas operaciones":
// "hoy", "mañana", "lunes" o, si falta una semana o más, "viernes 16"
export function etiquetaDia(fecha: Date, hoy: Date) {
  const dias = diasEntre(hoy, fecha);
  if (dias === 0) return "hoy";
  if (dias === 1) return "mañana";
  const nombre = fecha.toLocaleDateString("es-MX", { weekday: "long" });
  return dias < 7 ? nombre : `${nombre} ${fecha.getDate()}`;
}

// "2026-10-08T10:00:00" → "10:00"
export function formatoHora(texto: string) {
  const fecha = aFecha(texto);
  return `${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}`;
}