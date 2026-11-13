import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import Icono from "./Icono";

interface Props {
  titulo?: string; // Sin título se muestra la marca: "Montacargas Obregón."
  chip?: ReactNode; // Junto al título, por ejemplo el chip "Salida"
  atras?: boolean | string; // true regresa a la pantalla anterior; un texto lleva a esa ruta
  accion?: ReactNode; // Botón a la derecha
}

/** Barra de arriba. Variantes del prototipo: marca sola, atrás + marca, atrás + título + chip. */
export default function BarraSuperior({ titulo, chip, atras, accion }: Props) {
  const navigate = useNavigate();

  return (
    <header className="barra-superior">
      {atras && (
        <button
          type="button"
          className="boton-icono"
          aria-label="Regresar"
          onClick={() => (typeof atras === "string" ? navigate(atras) : navigate(-1))}
        >
          <Icono nombre="arrow_back" />
        </button>
      )}
      {titulo ? (
        <div className="barra-superior-titulo">
          <h1>{titulo}</h1>
          {chip}
        </div>
      ) : (
        <div className="barra-superior-marca">
          <span aria-hidden="true">🚜</span>
          <span>Montacargas Obregón.</span>
        </div>
      )}
      {accion && <div className="barra-superior-accion">{accion}</div>}
    </header>
  );
}