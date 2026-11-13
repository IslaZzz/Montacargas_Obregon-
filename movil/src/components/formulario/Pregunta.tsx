import type { ReactNode } from "react";
import Icono from "../Icono";

interface Props {
  etiqueta: string;
  obligatorio?: boolean;
  error?: string; // Pone el borde rojo y el mensaje abajo
  idCampo?: string; // Para enlazar la etiqueta con su campo
  children: ReactNode;
}

/** Recuadro de cada pregunta de un formulario: título con asterisco, el campo y su error. */
export default function Pregunta({ etiqueta, obligatorio, error, idCampo, children }: Props) {
  const Titulo = idCampo ? "label" : "p";
  return (
    <div className={`pregunta${error ? " pregunta-error" : ""}`}>
      <Titulo className="pregunta-titulo" {...(idCampo ? { htmlFor: idCampo } : {})}>
        {etiqueta}
        {obligatorio && <span className="obligatorio"> *</span>}
      </Titulo>
      {children}
      {error && (
        <p className="pregunta-mensaje" role="alert">
          <Icono nombre="error" relleno />
          {error}
        </p>
      )}
    </div>
  );
}