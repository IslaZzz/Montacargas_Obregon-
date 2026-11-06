import Chip from "../../components/chips";
import type { EstadoCarta } from "./cartasTypes";
import { CHIP_ESTADO, NOMBRE_ESTADO } from "./cartasUtils";

// Etiqueta "Carta: Generada" de la renta
export default function ChipCarta({ estado }: { estado: EstadoCarta }) {
  const { category, status } = CHIP_ESTADO[estado];
  return (
    <Chip category={category} status={status}>
      Carta: {NOMBRE_ESTADO[estado]}
    </Chip>
  );
}
