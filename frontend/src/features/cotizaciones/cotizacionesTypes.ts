import type { DatosDireccion } from "../../types/comun";
import type { Cliente, RazonSocial } from "../clientes/clientesTypes";

// Los estados de EstadoCotizacion que se usan en Delivery S1 (En autorización, Rechazada y Vencida llegan con HU-16, HU-32 y HU-17)
export type EstadoCotizacion = "BORRADOR" | "ENVIADA" | "ACEPTADA" | "NO_ACEPTADA";

// Equivale a CotizacionResumenResponse (una fila de la lista)
export interface CotizacionResumen {
  id: number;
  folio: string;
  cliente: string;
  empresa: string | null;
  fechaCreacion: string;
  fechaEntrega: string;
  fechaRecoleccion: string;
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
  direccionObra: DatosDireccion;
  contactoObra: string;
  telefonoContacto: string;
  partidas: PartidaCotizacion[];
  subtotal: number;
  iva: number;
  total: number;
}
