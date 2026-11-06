// Formatos de pesos, fechas y teléfonos como se muestran en las pantallas

const pesos = new Intl.NumberFormat("es-MX", {
  style: "currency",
  currency: "MXN",
});

// 9860 → "$9,860.00"
export const formatoMoneda = (monto: number) => pesos.format(monto);

// Convierte "2026-10-08" o "2026-10-08T09:00:00" (como llegan del backend) en fecha local.
// new Date("2026-10-08") la tomaría en UTC y en Sonora se vería un día antes.
export function aFecha(texto: string): Date {
  const [fecha, hora = "00:00"] = texto.split("T");
  const [anio, mes, dia] = fecha.split("-").map(Number);
  const [horas, minutos] = hora.split(":").map(Number);
  return new Date(anio, mes - 1, dia, horas, minutos);
}

// → "08/10/2026"
export const formatoFecha = (texto: string) =>
  aFecha(texto).toLocaleDateString("es-MX", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

const dosDigitos = (n: number) => String(n).padStart(2, "0");

// → "Lunes 12 de octubre de 2026, 09:00 a. m."
export function formatoFechaHora(texto: string) {
  const fecha = aFecha(texto);
  const dia = fecha
    .toLocaleDateString("es-MX", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    })
    .replace(",", ""); // "lunes, 12 de..." → "lunes 12 de..."
  const horas = fecha.getHours();
  const hora = `${dosDigitos(horas % 12 || 12)}:${dosDigitos(fecha.getMinutes())} ${horas < 12 ? "a. m." : "p. m."}`;
  return `${dia.charAt(0).toUpperCase()}${dia.slice(1)}, ${hora}`;
}

// → "Jueves 08 de octubre"
export function formatoDia(fecha: Date) {
  const dia = fecha
    .toLocaleDateString("es-MX", {
      weekday: "long",
      day: "2-digit",
      month: "long",
    })
    .replace(",", ""); // "jueves, 08 de..." → "jueves 08 de..."
  return `${dia.charAt(0).toUpperCase()}${dia.slice(1)}`;
}

// "6872104455" → "687 210 4455"
export const formatoTelefono = (telefono: string) =>
  telefono.replace(/^(\d{3})(\d{3})(\d{4})$/, "$1 $2 $3");
