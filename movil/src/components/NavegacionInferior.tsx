import { NavLink } from "react-router-dom";
import { PESTANAS } from "../sesion/navegacion";
import { useSesion } from "../sesion/useSesion";
import Icono from "./Icono";

/** Pestañas de abajo según el tipo de personal (ver sesion/navegacion.ts). Con una sola no se muestra. */
export default function NavegacionInferior() {
  const { personal } = useSesion();
  const pestanas = personal ? PESTANAS[personal.tipo] : [];
  if (pestanas.length < 2) return null;

  return (
    <nav className="navegacion-inferior" aria-label="Secciones">
      {pestanas.map((p) => (
        <NavLink key={p.ruta} to={p.ruta} className="navegacion-pestana">
          <span className="navegacion-indicador">
            <Icono nombre={p.icono} />
          </span>
          {p.etiqueta}
        </NavLink>
      ))}
    </nav>
  );
}