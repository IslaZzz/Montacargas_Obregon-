import type { ReactNode } from 'react';
import '../styles/buttons-chips.css';

export type ChipCategory =
  | 'renta'
  | 'equipo'
  | 'carta'
  | 'traslado'
  | 'mantenimiento'
  | 'cotizacion'
  | 'revision';

export type ChipStatus =
  | 'programado'
  | 'en-curso'
  | 'disponible'
  | 'mantenimiento'
  | 'preparacion'
  | 'fuera-servicio'
  | 'enviada'
  | 'generada'
  | 'borrador'
  | 'pendiente'
  | 'translado'
  | 'ok'
  | 'sin-asignar'
  | 'asignado'
  | 'en-proceso'
  | 'finalizado'
  | 'cancelado'
  | 'aceptada'
  | 'no-aceptada'
  | 'salida'
  | 'regreso'
  | 'sin-novedad'
  | 'con-observaciones'
  | 'requiere-mantenimiento';

interface ChipProps {
  category: ChipCategory;
  status: ChipStatus;
  children?: ReactNode;
  className?: string;
}

const chipClasses: Record<ChipCategory, Partial<Record<ChipStatus, string>>> = {
  renta: {
    programado: 'chip-programado',
    'en-curso': 'chip-en-curso',
  },

  equipo: {
    disponible: 'chip-equipo-disponible',
    mantenimiento: 'chip-equipo-mantenimiento',
    preparacion: 'chip-equipo-preparacion',
    'fuera-servicio': 'chip-equipo-fuera-servicio',
  },

  carta: {
    enviada: 'chip-carta-enviada',
    generada: 'chip-carta-generada',
    borrador: 'chip-borrador',
  },

  traslado: {
    pendiente: 'chip-pendiente',
    translado: 'chip-translado',
    ok: 'chip-ok',
  },

  mantenimiento: {
    'sin-asignar': 'chip-mantenimiento-sin-asignar',
    asignado: 'chip-mantenimiento-asignado',
    'en-proceso': 'chip-mantenimiento-en-proceso',
    finalizado: 'chip-mantenimiento-finalizado',
    cancelado: 'chip-mantenimiento-cancelado',
  },

  cotizacion: {
    borrador: 'chip-cotizacion chip-cotizacion-borrador',
    enviada: 'chip-cotizacion chip-cotizacion-enviada',
    aceptada: 'chip-cotizacion chip-cotizacion-aceptada',
    'no-aceptada': 'chip-cotizacion chip-cotizacion-no-aceptada',
  },

  // Mismos colores que mantenimientos para los estados; los demás reutilizan clases existentes
  revision: {
    'sin-asignar': 'chip-mantenimiento-sin-asignar',
    asignado: 'chip-mantenimiento-asignado',
    'en-proceso': 'chip-mantenimiento-en-proceso',
    finalizado: 'chip-mantenimiento-finalizado',
    cancelado: 'chip-mantenimiento-cancelado',
    salida: 'chip-equipo-disponible',
    regreso: 'chip-carta-generada',
    'sin-novedad': 'chip-ok',
    'con-observaciones': 'chip-pendiente',
    'requiere-mantenimiento': 'chip-equipo-fuera-servicio',
  },
};

const defaultLabels: Record<ChipStatus, string> = {
  programado: 'Programado',
  'en-curso': 'En curso',
  disponible: 'Disponible',
  mantenimiento: 'Mantenimiento',
  preparacion: 'Preparación',
  'fuera-servicio': 'Fuera de servicio',
  enviada: 'Enviada',
  generada: 'Generada',
  borrador: 'Borrador',
  pendiente: 'Pendiente',
  translado: 'En traslado',
  ok: 'Completado',
  'sin-asignar': 'Sin asignar',
  asignado: 'Asignado',
  'en-proceso': 'En progreso',
  finalizado: 'Finalizado',
  cancelado: 'Cancelado',
  aceptada: 'Aceptada',
  'no-aceptada': 'No aceptada',
  salida: 'Salida',
  regreso: 'Regreso',
  'sin-novedad': 'Sin novedad',
  'con-observaciones': 'Con observaciones',
  'requiere-mantenimiento': 'Requiere mantenimiento',
};

export default function Chip({
  category,
  status,
  children,
  className = '',
}: ChipProps) {
  const variantClass = chipClasses[category][status];

  if (!variantClass) {
    console.error(
      `El estado "${status}" no pertenece a la categoría "${category}".`
    );

    return null;
  }

  return (
    <span className={`chip ${variantClass} ${className}`.trim()}>
      {children ?? defaultLabels[status]}
    </span>
  );
}