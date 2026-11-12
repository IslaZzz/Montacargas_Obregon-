import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import type { EstadoInicioSesion } from "./sesionTypes";
import { useSesion } from "./useSesion";

// Las pantallas internas solo se ven con sesión; si no hay, manda a «Iniciar sesión»
export default function RutaProtegida({ children }: { children: ReactNode }) {
  const { sesion, cerroSesion } = useSesion();
  const ubicacion = useLocation();

  // Mientras se revisa la sesión no se muestra nada
  if (sesion === undefined) return null;

  if (sesion === null) {
    const estado: EstadoInicioSesion = cerroSesion
      ? { motivo: "cerrada" }
      : { motivo: "requerida", desde: ubicacion.pathname + ubicacion.search };
    return <Navigate to="/iniciar-sesion" replace state={estado} />;
  }

  return children;
}
