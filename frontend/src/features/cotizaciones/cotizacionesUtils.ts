import type { ChipStatus } from "../../components/chips";
import type { Cliente } from "../clientes/clientesTypes";
import type { EstadoCotizacion } from "./cotizacionesTypes";

// Estados en el orden de los filtros, con su texto y su chip
export const ESTADOS: {
  valor: EstadoCotizacion;
  etiqueta: string;
  chip: ChipStatus;
}[] = [
  { valor: "BORRADOR", etiqueta: "Borrador", chip: "borrador" },
  { valor: "ENVIADA", etiqueta: "Enviada", chip: "enviada" },
  { valor: "ACEPTADA", etiqueta: "Aceptada", chip: "aceptada" },
  { valor: "NO_ACEPTADA", etiqueta: "No aceptada", chip: "no-aceptada" },
];

export const chipDeEstado = (estado: EstadoCotizacion) =>
  ESTADOS.find((e) => e.valor === estado)!.chip;

// "Ramón Gutiérrez Soto"
export const nombreCompleto = (c: Cliente) =>
  [c.nombre, c.apellidoPaterno, c.apellidoMaterno].filter(Boolean).join(" ");

// "1 día", "5 días"
export const textoDias = (dias: number) => `${dias} ${dias === 1 ? "día" : "días"}`;
