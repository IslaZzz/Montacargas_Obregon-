import type { DatosDireccion } from "../../types/comun";
import type { Cliente, RazonSocial } from "../clientes/clientesTypes";
import type { EstadoContrato } from "../contratos/contratosTypes";
import type { EstadoRenta } from "../rentas/detalle/detalleTypes";

// Estados de una cotización. Rechazada es cuando gerencia niega el descuento; Vencida es cuando pasa su vigencia
export type EstadoCotizacion =
  | "BORRADOR"
  | "ENVIADA"
  | "ACEPTADA"
  | "NO_ACEPTADA"
  | "EN_AUTORIZACION"
  | "RECHAZADA"
  | "VENCIDA";

// Equivale a TipoDescuento del backend
export type TipoDescuento = "NINGUNO" | "PORCENTAJE" | "MONTO";

// Equivale a ResultadoAutorizacion del backend
export type ResultadoAutorizacion = "PENDIENTE" | "AUTORIZADA" | "RECHAZADA";

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
  enOtraCotizacionAceptada: boolean; // Está en el contrato Apartado de otra cotización, en las mismas fechas (HU-23)
}

// Equivale a RentaDeCotizacionResponse: la renta que salió de la cotización
export interface RentaDeCotizacion {
  folio: string;
  estado: EstadoRenta;
}

// Equivale a ContratoDeCotizacionResponse: el contrato que se generó al aceptarla (HU-23)
export interface ContratoDeCotizacion {
  id: number;
  folio: string;
  estado: EstadoContrato;
}

// Equivale a AutorizacionDescuentoResponse: la solicitud de autorización del descuento y su respuesta
export interface AutorizacionDescuento {
  resultado: ResultadoAutorizacion;
  solicitadaPor: string;
  fechaSolicitud: string;
  respondidaPor: string | null; // null mientras está pendiente
  fechaRespuesta: string | null; // null mientras está pendiente
  comentario: string | null; // Obligatorio al rechazar; opcional al autorizar
}

// Equivale a AutorizacionResumenResponse (una fila de la pantalla de gerencia)
export interface AutorizacionResumen {
  cotizacionId: number;
  folio: string;
  cliente: string;
  empresa: string | null;
  descuentoTipo: TipoDescuento;
  descuentoValor: number;
  descuentoMonto: number; // Pesos que se descuentan
  descuentoMotivo: string;
  total: number;
  autorizacion: AutorizacionDescuento;
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
  subtotal: number; // Equipos + traslados, antes del descuento
  descuentoTipo: TipoDescuento; // NINGUNO si no hay descuento
  descuentoValor: number | null; // Porcentaje o pesos, según el tipo; null con NINGUNO
  descuentoMotivo: string | null; // null con NINGUNO
  descuentoMonto: number; // Pesos que se descuentan; 0 si no hay
  iva: number;
  total: number;
  validaHasta: string; // "2026-10-15": último día para que el cliente la acepte
  diasVigencia: number;
  autorizacion: AutorizacionDescuento | null; // null si la cotización nunca pidió autorización
  renta: RentaDeCotizacion | null;
  contrato: ContratoDeCotizacion | null;
}

// Cuerpo de PATCH /cotizaciones/{id}/traslado
export interface TrasladoRequest {
  trasladoEntrega: number;
  trasladoRecoleccion: number;
}

// Cuerpo de PATCH /cotizaciones/{id}/descuento. Con NINGUNO, valor y motivo van en null
export interface DescuentoRequest {
  tipo: TipoDescuento;
  valor: number | null;
  motivo: string | null;
}