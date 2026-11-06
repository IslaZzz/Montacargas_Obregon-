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
