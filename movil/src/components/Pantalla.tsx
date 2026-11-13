import type { ReactNode } from "react";
import NavegacionInferior from "./NavegacionInferior";
import Notificacion from "./Notificacion";

interface Props {
  barraSuperior: ReactNode; // <BarraSuperior … />
  children: ReactNode;
  barraAccion?: ReactNode; // <BarraAccion … /> fija abajo, para formularios y detalles
  navegacion?: boolean; // Pestañas de abajo; se ocultan en detalles y formularios
}

/** Estructura de toda pantalla: barra de arriba, contenido con scroll y barras fijas abajo. */
export default function Pantalla({ barraSuperior, children, barraAccion, navegacion = false }: Props) {
  return (
    <div className="pantalla">
      {barraSuperior}
      <main className="pantalla-contenido">{children}</main>
      <div className="pantalla-abajo">
        <Notificacion />
        {barraAccion}
        {navegacion && <NavegacionInferior />}
      </div>
    </div>
  );
}