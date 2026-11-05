import { formatoMoneda } from "../../../utils/formato";

export const TASA_IVA_PORCENTAJE = 16;

const MS_POR_DIA = 86_400_000;

const aUtc = (fecha: string): number => {
  const [anio, mes, dia] = fecha.split("-").map(Number);
  return Date.UTC(anio, mes - 1, dia);
};

export const fechaHoraIso = (fecha: string, hora: string): string => `${fecha}T${hora}:00`;

export function calcularDias(entrega: string, recoleccion: string): number | null {
  if (!entrega || !recoleccion) return null;
  const inicio = aUtc(entrega);
  const fin = aUtc(recoleccion);
  if (Number.isNaN(inicio) || Number.isNaN(fin) || fin < inicio) return null;
  return Math.round((fin - inicio) / MS_POR_DIA) + 1;
}

export const aCentavos = (pesos: number): number => Math.round(pesos * 100);

export const importeCentavos = (dias: number, tarifaPorDia: number): number =>
  Math.round(dias * tarifaPorDia * 100);

export interface Totales {
  subtotal: number;
  iva: number;
  total: number;
}

export function calcularTotales(importes: number[]): Totales {
  const subtotal = importes.reduce((suma, importe) => suma + importe, 0);
  const iva = Math.round((subtotal * TASA_IVA_PORCENTAJE) / 100);
  return { subtotal, iva, total: subtotal + iva };
}

export const enPesos = (centavos: number): string => formatoMoneda(centavos / 100);
