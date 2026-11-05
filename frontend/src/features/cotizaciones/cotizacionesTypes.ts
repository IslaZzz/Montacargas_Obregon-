import type { Cliente, RazonSocial } from "../clientes/clientesTypes";

// Equivale a EstadoCotizacion
export type EstadoCotizacion = "BORRADOR" | "ENVIADA" | "ACEPTADA" | "NO_ACEPTADA";

// Equivale a CotizacionResumenResponse (una fila de la lista)
export interface CotizacionResumen {
  id: number;
  folio: string;
  cliente: string;
  empresa: string | null;
  fechaCreacion: string;
  total: number;
  estado: EstadoCotizacion;
}

// Equivale a PartidaCotizacionResponse
export interface PartidaCotizacion {
  id: number;
  unidad: string;
  modelo: string;
  tarifaDia: number;
  importe: number;
}

// Equivale a CotizacionResponse (el detalle)
export interface Cotizacion {
  id: number;
  folio: string;
  estado: EstadoCotizacion;
  fechaCreacion: string;
  fechaEnvio: string | null;
  fechaRespuesta: string | null;
  cliente: Cliente;
  razonSocial: RazonSocial;
  fechaEntrega: string;
  fechaRecoleccion: string;
  dias: number;
  direccionObra: string;
  contactoObra: string;
  telefonoContacto: string;
  partidas: PartidaCotizacion[];
  subtotal: number;
  iva: number;
  total: number;
}
