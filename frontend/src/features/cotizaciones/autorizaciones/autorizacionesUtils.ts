import type { ChipStatus } from "../../../components/chips";
import { formatoFecha, formatoMoneda } from "../../../utils/formato";
import type { AutorizacionResumen, ResultadoAutorizacion } from "../../cotizaciones/cotizacionesTypes";

export const chipDeResultado = (resultado: ResultadoAutorizacion): ChipStatus =>
  resultado === "PENDIENTE" ? "en-autorizacion" : resultado === "AUTORIZADA" ? "autorizada" : "rechazada";

const esHoy = (iso: string) => new Date(iso).toDateString() === new Date().toDateString();

// "10:42 a. m."
const hora = (iso: string) =>
  new Date(iso)
    .toLocaleTimeString("es-MX", { hour: "numeric", minute: "2-digit", hour12: true })
    .replace(/\s+/g, " ")
    .replace(/([ap])\.?\s?m\.?/i, (_, letra: string) => `${letra.toLowerCase()}. m.`);

// "Hoy, 10:42 a. m." o "05/10/2026, 4:10 p. m."
const cuando = (iso: string) => `${esHoy(iso) ? "Hoy" : formatoFecha(iso)}, ${hora(iso)}`;

// "Descuento: 10% (−$905.00)" o "Descuento: $1,000.00"
export const textoDescuento = (a: AutorizacionResumen) =>
  a.descuentoTipo === "PORCENTAJE"
    ? `${a.descuentoValor}% (−${formatoMoneda(a.descuentoMonto)})`
    : formatoMoneda(a.descuentoMonto);

export function lineaSolicitud(a: AutorizacionResumen) {
  const s = a.autorizacion;
  return s.resultado === "PENDIENTE"
    ? `Pidió: ${s.solicitadaPor} · ${cuando(s.fechaSolicitud)}`
    : `Pidió: ${s.solicitadaPor} · Solicitada el ${formatoFecha(s.fechaSolicitud)}`;
}

// El motivo si espera respuesta; si no, quién respondió y qué
export function lineaRespuesta(a: AutorizacionResumen) {
  const s = a.autorizacion;
  if (s.resultado === "PENDIENTE") return `Motivo: ${a.descuentoMotivo}`;
  if (s.resultado === "AUTORIZADA") return `Respondió: ${s.respondidaPor} (gerencia) · ${cuando(s.fechaRespuesta!)}`;
  return `Rechazada: «${s.comentario}» · ${s.respondidaPor}, ${formatoFecha(s.fechaRespuesta!)}`;
}

// "Lo pidió Juan Pérez hoy a las 10:42 a. m."
export function lineaLoPidio(solicitadaPor: string, fechaSolicitud: string) {
  const dia = esHoy(fechaSolicitud) ? "hoy" : `el ${formatoFecha(fechaSolicitud)}`;
  return `Lo pidió ${solicitadaPor} ${dia} a las ${hora(fechaSolicitud)}`;
}