import type { ChipStatus } from "../../components/chips";
import { aFecha } from "../../utils/formato";
import type { Cliente } from "../clientes/clientesTypes";
import type { EstadoRenta } from "../rentas/detalle/detalleTypes";
import type { Cotizacion, EstadoCotizacion } from "./cotizacionesTypes";

// Estados en el orden de los filtros, con su texto y su chip
export const ESTADOS: {
  valor: EstadoCotizacion;
  etiqueta: string;
  chip: ChipStatus;
}[] = [
  { valor: "BORRADOR", etiqueta: "Borrador", chip: "borrador" },
  { valor: "EN_AUTORIZACION", etiqueta: "En autorización", chip: "en-autorizacion" },
  { valor: "ENVIADA", etiqueta: "Enviada", chip: "enviada" },
  { valor: "ACEPTADA", etiqueta: "Aceptada", chip: "aceptada" },
  { valor: "NO_ACEPTADA", etiqueta: "No aceptada", chip: "no-aceptada" },
  { valor: "RECHAZADA", etiqueta: "Rechazada", chip: "rechazada" },
  { valor: "VENCIDA", etiqueta: "Vencida", chip: "vencida" },
];

export const chipDeEstado = (estado: EstadoCotizacion) =>
  ESTADOS.find((e) => e.valor === estado)!.chip;

// Texto del estado de la renta: "Programada"
export const ESTADOS_RENTA: Record<EstadoRenta, string> = {
  PROGRAMADA: "Programada",
  EN_CURSO: "En curso",
  CERRADA: "Cerrada",
  CANCELADA: "Cancelada",
};

// "Ramón Gutiérrez Soto"
export const nombreCompleto = (c: Cliente) =>
  [c.nombre, c.apellidoPaterno, c.apellidoMaterno].filter(Boolean).join(" ");

export function motivoSinPdf(c: Cotizacion): string | null {
  if (c.partidas.length === 0) return "Agrega al menos un equipo";
  if (c.estado === "EN_AUTORIZACION") return "Esperando autorización";
  return null;
}

// "1 día", "5 días"
export const textoDias = (dias: number) => `${dias} ${dias === 1 ? "día" : "días"}`;

export const DIAS_CANCELACION = 3;

export function textoCancelacion(fechaEntrega: string) {
  const limite = aFecha(fechaEntrega);
  limite.setDate(limite.getDate() - DIAS_CANCELACION);
  const dia = limite
    .toLocaleDateString("es-MX", { weekday: "long", day: "numeric", month: "long" })
    .replace(",", "");
  return `Sin costo hasta ${DIAS_CANCELACION} días antes de la entrega (hasta el ${dia}). Con menos tiempo se pierde el anticipo.`;
}

export const TEXTO_CARTA_RESPONSIVA = "Se firma al recibir el equipo.";
