import { useId } from "react";
import type { InputHTMLAttributes } from "react";
import Pregunta from "./Pregunta";

interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, "onChange" | "value"> {
  etiqueta: string;
  valor: string;
  onCambio: (valor: string) => void;
  obligatorio?: boolean;
  error?: string;
}

/** Texto o número en una línea: "Horómetro (h)". Para decimales usa inputMode="decimal". */
export default function CampoTexto({ etiqueta, valor, onCambio, obligatorio, error, ...resto }: Props) {
  const id = useId();
  return (
    <Pregunta etiqueta={etiqueta} obligatorio={obligatorio} error={error} idCampo={id}>
      <input
        {...resto}
        id={id}
        className="entrada"
        value={valor}
        aria-invalid={Boolean(error)}
        onChange={(e) => onCambio(e.target.value)}
      />
    </Pregunta>
  );
}