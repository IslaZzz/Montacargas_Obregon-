import { useCallback, useEffect, useMemo, useState, useSyncExternalStore } from "react";
import { listarApartadosProximos } from "./avisosApi";
import type { ApartadoProximo } from "./avisosTypes";
import { ANTICIPACION_INICIAL, ANTICIPACIONES, claveAviso } from "./avisosUtils";

// Cada cuánto se vuelven a pedir los avisos
const INTERVALO_MS = 60_000;

// Provisional mientras no haya inicio de sesión: la anticipación y los avisos leídos
// se guardan en este navegador. Cuando exista el usuario, deberían guardarse en el backend.
const CLAVE_ANTICIPACION = "avisos.anticipacion";
const CLAVE_LEIDOS = "avisos.leidos";
const MAXIMO_LEIDOS = 200;

// Avisa a la campana y a la pantalla cuando una de las dos cambia algo
const EVENTO = "avisos-cambio";

function leer(clave: string) {
  try {
    return localStorage.getItem(clave);
  } catch {
    return null; // Navegador sin almacenamiento (por ejemplo, ventana privada)
  }
}

function guardar(clave: string, valor: string) {
  try {
    localStorage.setItem(clave, valor);
  } catch {
    // Si no se puede guardar, solo dura mientras la pantalla esté abierta
  }
  window.dispatchEvent(new Event(EVENTO));
}

function suscribir(avisar: () => void) {
  window.addEventListener(EVENTO, avisar);
  window.addEventListener("storage", avisar); // Cambios hechos en otra pestaña
  return () => {
    window.removeEventListener(EVENTO, avisar);
    window.removeEventListener("storage", avisar);
  };
}

function leerAnticipacion() {
  const dias = Number(leer(CLAVE_ANTICIPACION));
  return ANTICIPACIONES.includes(dias) ? dias : ANTICIPACION_INICIAL;
}

export function useAnticipacion() {
  const dias = useSyncExternalStore(suscribir, leerAnticipacion);
  const cambiar = useCallback((nuevos: number) => guardar(CLAVE_ANTICIPACION, String(nuevos)), []);
  return [dias, cambiar] as const;
}

function useLeidos() {
  const texto = useSyncExternalStore(suscribir, () => leer(CLAVE_LEIDOS));
  return useMemo(() => {
    try {
      const lista: unknown = JSON.parse(texto ?? "[]");
      return Array.isArray(lista) ? lista.filter((c) => typeof c === "string") : [];
    } catch {
      return [];
    }
  }, [texto]);
}

// Los apartados próximos con la anticipación elegida; se actualizan solos cada minuto
export function useApartadosProximos() {
  const [dias] = useAnticipacion();
  const leidos = useLeidos();
  const [apartados, setApartados] = useState<ApartadoProximo[] | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let activo = true;
    const cargar = () =>
      listarApartadosProximos(dias)
        .then((lista) => {
          if (!activo) return;
          setApartados(lista);
          setError(false);
        })
        .catch(() => {
          if (activo) setError(true);
        });

    cargar();
    const intervalo = setInterval(cargar, INTERVALO_MS);
    return () => {
      activo = false;
      clearInterval(intervalo);
    };
  }, [dias]);

  const noLeidos = useMemo(() => {
    const vistos = new Set(leidos);
    return (apartados ?? []).filter((a) => !vistos.has(claveAviso(a)));
  }, [apartados, leidos]);

  // Los avisos marcados ya no cuentan en la campana, aunque sigan en la lista
  const marcarComoLeidos = useCallback(() => {
    const nuevos = (apartados ?? []).map(claveAviso);
    const todos = [...new Set([...leidos, ...nuevos])].slice(-MAXIMO_LEIDOS);
    guardar(CLAVE_LEIDOS, JSON.stringify(todos));
  }, [apartados, leidos]);

  return { dias, apartados, error, noLeidos, marcarComoLeidos };
}
