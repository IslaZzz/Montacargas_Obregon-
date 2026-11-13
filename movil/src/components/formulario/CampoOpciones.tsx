import { useId } from "react";
import Pregunta from "./Pregunta";

export interface Opcion<T extends string> {
  valor: T;
  etiqueta: string;
  tono?: "normal" | "atencion" | "falla"; // Color al elegirla
  pideDetalle?: boolean; // Muestra un campo para explicar: "Desgaste en llanta delantera"
}

interface Props<T extends string> {
  etiqueta: string;
  opciones: Opcion<T>[];
  valor: T | null;
  onCambio: (valor: T) => void;
  detalle?: string;
  onDetalle?: (texto: string) => void;
  placeholderDetalle?: string;
  obligatorio?: boolean;
  error?: string;
}

/** Elegir una opción de una lista, con detalle opcional: Bien · Atención · Falla */
export default function CampoOpciones<T extends string>(props: Props<T>) {
  const { etiqueta, opciones, valor, onCambio, detalle = "", onDetalle, placeholderDetalle = "Describe lo que encontraste", obligatorio, error } = props;
  const nombre = useId();
  const elegida = opciones.find((o) => o.valor === valor);

  return (
    <Pregunta etiqueta={etiqueta} obligatorio={obligatorio} error={error}>
      <div className="opciones" role="radiogroup" aria-label={etiqueta}>
        {opciones.map((o) => (
          <label key={o.valor} className={`opcion opcion-${o.tono ?? "normal"}`}>
            <input type="radio" name={nombre} checked={o.valor === valor} onChange={() => onCambio(o.valor)} />
            <span className="opcion-marca" aria-hidden="true" />
            {o.etiqueta}
          </label>
        ))}
      </div>
      {elegida?.pideDetalle && onDetalle && (
        <input
          className={`entrada entrada-${elegida.tono ?? "normal"}`}
          aria-label={`Detalle de ${etiqueta}`}
          placeholder={placeholderDetalle}
          maxLength={255}
          value={detalle}
          onChange={(e) => onDetalle(e.target.value)}
        />
      )}
    </Pregunta>
  );
}