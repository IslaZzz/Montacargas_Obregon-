/** Texto cuando una lista no tiene nada: "No hay mantenimientos en curso." */
export default function EstadoVacio({ children }: { children: string }) {
  return <p className="estado-vacio">{children}</p>;
}