import type { InputHTMLAttributes } from "react";

interface CampoProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "onChange"
> {
  etiqueta: string;
  obligatorio?: boolean;
  error?: string;
  onChange: (valor: string) => void;
}

export default function Campo({
  etiqueta,
  obligatorio,
  error,
  onChange,
  ...resto
}: CampoProps) {
  return (
    <label className="campo">
      <span className="campo-etiqueta">
        {etiqueta} {obligatorio && <small>(obligatorio)</small>}
      </span>
      <input
        className={"campo-input" + (error ? " con-error" : "")}
        aria-invalid={error ? true : undefined}
        onChange={(e) => onChange(e.target.value)}
        {...resto}
      />
      {error && (
        <span className="campo-error" role="alert">
          <span className="material-symbols-rounded">error</span>
          {error}
        </span>
      )}
    </label>
  );
}