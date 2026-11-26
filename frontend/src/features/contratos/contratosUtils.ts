import type { ChipStatus } from "../../components/chips";
import type { EstadoContrato } from "./contratosTypes";

// Texto y chip de cada estado del contrato
export const ESTADOS_CONTRATO: Record<
  EstadoContrato,
  { etiqueta: string; chip: ChipStatus }
> = {
  APARTADO: { etiqueta: "Apartado", chip: "apartado" },
  APARTADO_VENCIDO: { etiqueta: "Apartado vencido", chip: "apartado-vencido" },
};

// Lo que dice el PDF del contrato después del anticipo (ContratoPdfService)
export const TITULO_POLITICA = "POLÍTICA DE CANCELACIÓN Y PENALIZACIONES";
export const POLITICA = [
  "Cancelación con 3 días o más antes de la entrega: se devuelve el anticipo completo.",
  "Cancelación con menos de 3 días: se pierde el anticipo como penalización.",
  "Si el anticipo no se paga antes de la fecha límite, el apartado vence y los equipos se liberan.",
  "El cliente firmará una carta responsiva al recibir el equipo.",
];
