import { useId } from "react";
import Pregunta from "./Pregunta";

interface Props {
  etiqueta: string; // "Notas (opcional)", "Mensaje para el auxiliar"
  valor: string;
  onCambio: (valor: string) => void;
  obligatorio?: boolean;
  error?: string;
  placeholder?: string;
  maximo?: number;
}

/** Texto libre de varias líneas. */
export default function CampoNota({ etiqueta, valor, onCambio, obligatorio, error, placeholder = "Escribe una nota…", maximo = 255 }: Props) {
  const id = useId();
  return (
    <Pregunta etiqueta={etiqueta} obligatorio={obligatorio} error={error} idCampo={id}>
      <textarea
        id={id}
        className="nota"
        rows={3}
        maxLength={maximo}
        placeholder={placeholder}
        value={valor}
        aria-invalid={Boolean(error)}
        onChange={(e) => onCambio(e.target.value)}
      />
    </Pregunta>
  );
}