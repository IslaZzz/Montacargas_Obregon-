import type { ReactNode } from "react";
import Icono from "./Icono";

// Colores del prototipo:
// verde: Entrega, Salida, Listo para entrega, Enviado, Atendida · lila: En camino, En traslado
// azul: En curso, En progreso · amarillo: Regreso, Nuevo, Pendiente · gris: Programada
// naranja: Firma de carta en sitio · rojo: Falla
export type TonoChip = "verde" | "lila" | "azul" | "amarillo" | "gris" | "naranja" | "rojo";

export default function Chip({ tono, icono, children }: { tono: TonoChip; icono?: string; children: ReactNode }) {
  return (
    <span className={`chip chip-${tono}`}>
      {icono && <Icono nombre={icono} />}
      {children}
    </span>
  );
}