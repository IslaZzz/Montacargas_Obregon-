import Chip, { claseDeChip } from "../../../components/chips";
import type { EstadoConsultaEquipo } from "./estadosTypes";

// Orden en que se muestran en los filtros y en el resumen
export const ESTADOS: EstadoConsultaEquipo[] = [
  "EN_RENTA",
  "DISPONIBLE",
  "EN_PREPARACION",
  "EN_MANTENIMIENTO",
  "FUERA_SERVICIO",
];

// Texto del filtro y del resumen
export const NOMBRE_ESTADO: Record<EstadoConsultaEquipo, string> = {
  DISPONIBLE: "Disponibles",
  EN_PREPARACION: "En preparación",
  EN_RENTA: "En renta",
  EN_MANTENIMIENTO: "En mantenimiento",
  FUERA_SERVICIO: "Fuera de servicio",
};

// Colores de cada estado; "En renta" usa los de la renta en curso
export const CLASE_ESTADO: Record<EstadoConsultaEquipo, string> = {
  DISPONIBLE: claseDeChip("equipo", "disponible"),
  EN_PREPARACION: claseDeChip("equipo", "preparacion"),
  EN_RENTA: claseDeChip("renta", "en-curso"),
  EN_MANTENIMIENTO: claseDeChip("equipo", "mantenimiento"),
  FUERA_SERVICIO: claseDeChip("equipo", "fuera-servicio"),
};

export function ChipEstadoEquipo({ estado }: { estado: EstadoConsultaEquipo }) {
  switch (estado) {
    case "DISPONIBLE":
      return <Chip category="equipo" status="disponible" />;
    case "EN_PREPARACION":
      return <Chip category="equipo" status="preparacion">En preparación</Chip>;
    case "EN_RENTA":
      return <Chip category="renta" status="en-curso">En renta</Chip>;
    case "EN_MANTENIMIENTO":
      return <Chip category="equipo" status="mantenimiento" />;
    case "FUERA_SERVICIO":
      return <Chip category="equipo" status="fuera-servicio" />;
  }
}