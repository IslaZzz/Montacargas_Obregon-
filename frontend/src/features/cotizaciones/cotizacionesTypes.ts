import type { DatosDireccion } from "../../types/comun";
import type { Cliente, RazonSocial } from "../clientes/clientesTypes";
import type { EstadoRenta } from "../rentas/detalle/detalleTypes";

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

// Equivale a RentaDeCotizacionResponse: la renta que nació al aceptar la cotización (HU-37)
export interface RentaDeCotizacion {
  folio: string;
  estado: EstadoRenta;
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
  trasladoEntrega: number; // 0 si no hay traslado
  trasladoRecoleccion: number; // 0 si no hay traslado
  precioDefinido: boolean; // True cuando ya se capturó el precio (hoy, al guardar el traslado). Con él, la cotización ya puede generar su PDF
  subtotal: number;
  iva: number;
  total: number;
  renta: RentaDeCotizacion | null;
}

// Cuerpo de PATCH /cotizaciones/{id}/traslado
export interface TrasladoRequest {
  trasladoEntrega: number;
  trasladoRecoleccion: number;
}