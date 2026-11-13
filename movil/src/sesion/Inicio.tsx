import { Navigate } from "react-router-dom";
import { INICIO } from "./navegacion";
import { useSesion } from "./useSesion";

/** "/": manda a cada quien a su sección. Va dentro de RutaProtegida, así que ya hay persona. */
export default function Inicio() {
  const { personal } = useSesion();
  return <Navigate to={personal ? INICIO[personal.tipo] : "/entrar"} replace />;
}