import type { Cotizacion } from "../cotizaciones/cotizacionesTypes";

// Equivale a EstadoContrato del backend. Contratado llega con HU-24 y Cancelado con HU-26
export type EstadoContrato = "APARTADO" | "APARTADO_VENCIDO";

// Equivale a ContratoResponse (el detalle). Cliente, equipos, fechas, lugar y precio vienen en la cotización
export interface Contrato {
  id: number;
  folio: string;
  estado: EstadoContrato;
  fechaGeneracion: string;
  fechaLimiteAnticipo: string; // "2026-10-12": si no se paga antes, ese día vence el apartado
  anticipo: number; // 30% del total con IVA
  cotizacion: Cotizacion;
}
