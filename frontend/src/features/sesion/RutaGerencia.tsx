import { Outlet } from "react-router-dom";
import SinPermiso from "./SinPermiso";
import { esGerencia } from "./sesionUtils";
import { useSesion } from "./useSesion";

// Las pantallas dentro de esta ruta son solo de gerencia (HU-29); el auxiliar ve «Sin permiso»
export default function RutaGerencia() {
  const { sesion } = useSesion();

  // RutaProtegida ya revisó que haya sesión
  if (!sesion) return null;

  return esGerencia(sesion.rol) ? <Outlet /> : <SinPermiso />;
}
