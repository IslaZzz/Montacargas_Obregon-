import type { ReactNode } from 'react';
import '../styles/buttons-chips.css';

export type ChipCategory =
  | 'renta'
  | 'equipo'
  | 'carta'
  | 'traslado';

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
  | 'ok';

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
