import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { EVENTO_SIN_SESION } from "../../services/apiClient";
import { cerrarSesion, iniciarSesion, obtenerSesion } from "./sesionApi";
import { SesionContexto } from "./sesionContexto";
import type { DatosInicioSesion, Sesion } from "./sesionTypes";

// Guarda quién tiene la sesión abierta para toda la aplicación (HU-28)
export default function SesionProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null | undefined>(undefined);
  const [cerroSesion, setCerroSesion] = useState(false);

  useEffect(() => {
    let vigente = true;
    // Al abrir la aplicación, revisa si ya había una sesión
    obtenerSesion()
      .then((s) => vigente && setSesion(s))
      .catch(() => vigente && setSesion(null));

    // Si el backend responde 401, la sesión venció
    const sinSesion = () => {
      setCerroSesion(false);
      setSesion(null);
    };
    window.addEventListener(EVENTO_SIN_SESION, sinSesion);
    return () => {
      vigente = false;
      window.removeEventListener(EVENTO_SIN_SESION, sinSesion);
    };
  }, []);

  const entrar = useCallback(async (datos: DatosInicioSesion) => {
    const nueva = await iniciarSesion(datos);
    if (!nueva) return false;
    setCerroSesion(false);
    setSesion(nueva);
    return true;
  }, []);

  const salir = useCallback(async () => {
    try {
      await cerrarSesion();
    } catch {
      // Sin conexión: de todos modos se sale de este lado
    }
    setCerroSesion(true);
    setSesion(null);
  }, []);

  const valor = useMemo(
    () => ({ sesion, cerroSesion, entrar, salir }),
    [sesion, cerroSesion, entrar, salir],
  );

  return <SesionContexto.Provider value={valor}>{children}</SesionContexto.Provider>;
}
