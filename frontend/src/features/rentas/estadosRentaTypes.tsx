export type EstadoRenta =
  | "PROGRAMADA"
  | "EN_CURSO"
  | "CERRADA"
  | "CANCELADA";

export type FaseEquipoRenta =
  | "RESERVADO"
  | "EN_REVISION_SALIDA"
  | "LISTO_ENTREGA"
  | "EN_TRASLADO"
  | "EN_OPERACION"
  | "EN_REVISION_ENTRADA"
  | "COMPLETADO"
  | "CANCELADO";

export interface ClienteRenta {
  id: number;
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  empresa: string;
}

export interface EquipoRenta {
  id: number;
  equipoId: number;
  numeroUnidad: string;
  numeroSerie: string;
  fase: FaseEquipoRenta;
}

export interface Renta {
  id: number;
  folio: string;
  cliente: ClienteRenta;
  fechaInicio: string;
  fechaFin: string;
  entregaDesde: string | null;
  entregaHasta: string | null;
  recoleccionDesde: string | null;
  recoleccionHasta: string | null;
  direccionObra: string;
  contactoObra: string;
  telefonoContacto: string;
  estado: EstadoRenta;
  equipos: EquipoRenta[];
}
