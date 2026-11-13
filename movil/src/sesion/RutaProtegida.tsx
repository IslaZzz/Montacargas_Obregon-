import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import Cargando from "../components/Cargando";
import { INICIO } from "./navegacion";
import type { TipoPersonal } from "./sesionTypes";
import { useSesion } from "./useSesion";

/** Pide elegir quién eres; si la sección es de otro tipo de personal, manda a su inicio. */
export default function RutaProtegida({ tipo, children }: { tipo?: TipoPersonal; children: ReactNode }) {
  const { personal, cargando } = useSesion();
  const ubicacion = useLocation();

  if (cargando) return <Cargando pantallaCompleta />;
  if (!personal) return <Navigate to="/entrar" replace state={{ desde: ubicacion.pathname }} />;
  if (tipo && personal.tipo !== tipo) return <Navigate to={INICIO[personal.tipo]} replace />;
  return <>{children}</>;
}