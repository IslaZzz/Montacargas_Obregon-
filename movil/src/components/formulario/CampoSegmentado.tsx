import Pregunta from "./Pregunta";

interface Props<T extends string> {
  etiqueta: string;
  opciones: { valor: T; etiqueta: string }[];
  valor: T | null;
  onCambio: (valor: T) => void;
  obligatorio?: boolean;
  error?: string;
}

/** Una opción de pocas y cortas, en botones: Combustible 1/4 · 1/2 · 3/4 · Lleno */
export default function CampoSegmentado<T extends string>({ etiqueta, opciones, valor, onCambio, obligatorio, error }: Props<T>) {
  return (
    <Pregunta etiqueta={etiqueta} obligatorio={obligatorio} error={error}>
      <div className="segmentado" role="radiogroup" aria-label={etiqueta}>
        {opciones.map((o) => (
          <button
            key={o.valor}
            type="button"
            role="radio"
            aria-checked={o.valor === valor}
            className="segmento"
            onClick={() => onCambio(o.valor)}
          >
            {o.etiqueta}
          </button>
        ))}
      </div>
    </Pregunta>
  );
}