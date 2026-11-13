import type { ReactNode } from "react";

/** Lista de datos dentro de una tarjeta: "Traslado ……… TR-0089" */
export function ListaDatos({ children }: { children: ReactNode }) {
  return <dl className="datos">{children}</dl>;
}

/**
 * Un dato. En renglón: etiqueta a la izquierda y valor a la derecha.
 * apilado: etiqueta arriba y valor abajo, para textos largos (trabajo realizado, notas).
 */
export function Dato({ etiqueta, children, apilado = false }: { etiqueta: string; children: ReactNode; apilado?: boolean }) {
  return (
    <div className={`dato${apilado ? " dato-apilado" : ""}`}>
      <dt>{etiqueta}</dt>
      <dd>{children}</dd>
    </div>
  );
}