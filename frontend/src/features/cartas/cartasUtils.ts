import type { ChipCategory, ChipStatus } from "../../components/chips";
import { formatoDiaCorto, formatoHora } from "../../utils/fechas";
import type { Carta, EstadoCarta } from "./cartasTypes";

export const NOMBRE_ESTADO: Record<EstadoCarta, string> = {
  SIN_GENERAR: "Sin generar",
  GENERADA: "Generada",
  ENVIADA: "Enviada",
  FIRMADA: "Firmada",
};

// El Chip de carta no tiene "sin generar" ni "firmada": se usan el gris de borrador
// y el verde de completado, con el texto de la carta
export const CHIP_ESTADO: Record<EstadoCarta, { category: ChipCategory; status: ChipStatus }> = {
  SIN_GENERAR: { category: "carta", status: "borrador" },
  GENERADA: { category: "carta", status: "generada" },
  ENVIADA: { category: "carta", status: "enviada" },
  FIRMADA: { category: "traslado", status: "ok" },
};

// "2026-10-09T10:42:00" → "9 oct, 10:42"
export function textoFecha(fecha: string) {
  const [, dia, mes] = formatoDiaCorto(fecha).split(" "); // "Vie 9 oct"
  return `${dia} ${mes}, ${formatoHora(fecha)}`;
}

const DIAS_LARGOS = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];
const MESES_LARGOS = [
  "enero", "febrero", "marzo", "abril", "mayo", "junio",
  "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
];

// "2026-10-09T12:30:00" → "12:30 p.m."
export function textoHora12(fecha: string) {
  const f = new Date(fecha);
  const horas = f.getHours() % 12 || 12;
  return `${horas}:${String(f.getMinutes()).padStart(2, "0")} ${f.getHours() < 12 ? "a.m." : "p.m."}`;
}

// "2026-10-09T12:30:00" → "Viernes 9 de octubre · 12:30 p.m."
export function textoFechaLarga(fecha: string) {
  const f = new Date(fecha);
  return `${DIAS_LARGOS[f.getDay()]} ${f.getDate()} de ${MESES_LARGOS[f.getMonth()]} · ${textoHora12(fecha)}`;
}

// La fecha y hora de ahora como la pide <input type="datetime-local">: "2026-10-09T12:30"
export function ahoraParaInput() {
  const f = new Date();
  f.setMinutes(f.getMinutes() - f.getTimezoneOffset());
  return f.toISOString().slice(0, 16);
}

// 190464 → "186 KB"; 2516582 → "2.4 MB"
export function textoTamano(bytes: number) {
  return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

export const NOMBRE_TIPO_FIRMA = { ELECTRONICA: "Firma electrónica", FISICA: "Firma física" } as const;

// Lo mismo que revisa el backend: PDF, JPG o PNG (solo PDF si es electrónica) y hasta 10 MB
export const MAXIMO_ARCHIVO_FIRMADO = 10 * 1024 * 1024;
export const TIPOS_FIRMA = {
  ELECTRONICA: ["application/pdf"],
  FISICA: ["application/pdf", "image/jpeg", "image/png"],
} as const;

// → "v1 · 9 oct, 10:42"
export const textoVersion = (carta: Carta) =>
  carta.fechaGeneracion ? `v${carta.version} · ${textoFecha(carta.fechaGeneracion)}` : `v${carta.version}`;

// Abre el diálogo de imprimir del navegador con el PDF, sin salir de la pantalla.
// Si el navegador no lo permite (algunos celulares), abre el PDF en otra pestaña para imprimirlo desde ahí.
export function imprimirPdf(url: string) {
  const marco = document.createElement("iframe");
  marco.className = "carta-marco-impresion";
  marco.title = "Carta para imprimir";
  marco.src = url;
  marco.onload = () => {
    try {
      marco.contentWindow?.focus();
      marco.contentWindow?.print();
    } catch {
      window.open(url, "_blank", "noopener");
    }
    // Se quita después: si se quita de inmediato, se cancela la impresión
    setTimeout(() => marco.remove(), 60_000);
  };
  document.body.appendChild(marco);
}
