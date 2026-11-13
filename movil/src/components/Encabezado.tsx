import type { ReactNode } from "react";

/** Tarjeta de título con la franja oscura arriba: "Mis tareas", "Revisión de salida"… */
export default function Encabezado({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return (
    <section className="encabezado">
      <h2>{titulo}</h2>
      {children && <div className="encabezado-extra">{children}</div>}
    </section>
  );
}