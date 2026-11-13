import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import Icono from "./Icono";

type Borde = "normal" | "atencion" | "error";

/** Recuadro con borde. "atencion" (naranja) y "error" (rojo) marcan algo pendiente o con falla. */
export function Tarjeta({ children, borde = "normal", className = "" }: { children: ReactNode; borde?: Borde; className?: string }) {
  return <section className={`tarjeta tarjeta-${borde} ${className}`}>{children}</section>;
}

/** Tarjeta de lista que abre un detalle: todo el recuadro es el enlace y lleva flecha a la derecha. */
export function TarjetaEnlace({ to, children, borde = "normal" }: { to: string; children: ReactNode; borde?: Borde }) {
  return (
    <Link to={to} className={`tarjeta tarjeta-${borde} tarjeta-enlace`}>
      <div className="tarjeta-enlace-cuerpo">{children}</div>
      <Icono nombre="chevron_right" className="tarjeta-enlace-flecha" />
    </Link>
  );
}

/** Título grande de una tarjeta de lista: "EQ-031", "Constructora del Yaqui" */
export const TituloTarjeta = ({ children }: { children: ReactNode }) => <h3 className="tarjeta-titulo">{children}</h3>;

/** Renglón con ícono dentro de una tarjeta: hora, lugar, número de equipos… */
export function LineaTarjeta({ icono, children }: { icono: string; children: ReactNode }) {
  return (
    <p className="tarjeta-linea">
      <Icono nombre={icono} />
      <span>{children}</span>
    </p>
  );
}