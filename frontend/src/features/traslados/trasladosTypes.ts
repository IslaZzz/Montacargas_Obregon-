export type TipoTraslado = 'ENTREGA' | 'RECOLECCION';
export type EstadoTraslado = 'PROGRAMADO' | 'EN_CURSO' | 'COMPLETADO' | 'CANCELADO';

export type FiltroTraslados = 'TODOS' | 'SIN_ASIGNAR' | 'ENTREGAS' | 'RECOLECCIONES';

export interface Traslado {
  id: number;
  folio: string;
  rentaId: number;
  rentaFolio: string;
  tipo: TipoTraslado;
  fechaHoraInicio: string; // ISO string
  fechaHoraFin: string;    // ISO string
  clienteNombre: string;
  direccion: string;
  contacto: string;
  telefono: string;
  responsableId: number | null;
  responsableCodigo: string | null;
  responsableNombre: string | null;
  responsableIniciales: string | null;
  estado: EstadoTraslado;
  yaInicio: boolean;
}

export interface OperadorDisponibilidad {
  id: number;
  codigo: string;
  nombreCompleto: string;
  iniciales: string;
  disponible: boolean;
  motivo: string;
  conflictoTrasladoId?: number | null;
  conflictoTrasladoFolio?: string | null;
}

export interface AsignarTrasladoPayload {
  personalId: number;
}

