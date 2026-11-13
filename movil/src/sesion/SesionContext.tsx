import { createContext, useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { ErrorApi } from "../services/api";
import { guardarPersonalId, leerPersonalId } from "./almacen";
import { quienSoy } from "./sesionApi";
import type { Personal } from "./sesionTypes";

interface Sesion {
  personal: Personal | null;
  cargando: boolean; // Mientras se confirma la persona guardada
  entrar: (personal: Personal) => void;
  salir: () => void;
}

export const SesionContext = createContext<Sesion | null>(null);

/**
 * Quién usa la app. Mientras no exista el inicio de sesión (HU-28), la persona se elige
 * de una lista y se guarda en el celular; cada petición la manda en X-Personal-Id.
 */
export function SesionProvider({ children }: { children: ReactNode }) {
  const [personal, setPersonal] = useState<Personal | null>(null);
  const [cargando, setCargando] = useState(() => leerPersonalId() !== null);

  useEffect(() => {
    if (leerPersonalId() === null) return;
    quienSoy()
      .then(setPersonal)
      .catch((error) => {
        // Solo se olvida si el backend dice que ya no es válida; sin conexión se conserva
        if (error instanceof ErrorApi && error.estado === 401) guardarPersonalId(null);
      })
      .finally(() => setCargando(false));
  }, []);

  const entrar = useCallback((nuevo: Personal) => {
    guardarPersonalId(nuevo.id);
    setPersonal(nuevo);
  }, []);

  const salir = useCallback(() => {
    guardarPersonalId(null);
    setPersonal(null);
  }, []);

  const valor = useMemo(() => ({ personal, cargando, entrar, salir }), [personal, cargando, entrar, salir]);
  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>;
}