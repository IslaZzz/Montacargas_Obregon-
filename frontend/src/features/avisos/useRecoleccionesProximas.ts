import { useCallback, useEffect, useMemo, useState } from "react";
import { listarRecoleccionesProximas } from "./avisosApi";
import type { RecoleccionProxima } from "./avisosTypes";
import { useAnticipacion } from "./useApartadosProximos";

// Cada cuánto se vuelven a pedir los recordatorios
const INTERVALO_MS = 60_000;

// Igual que los apartados próximos: lo leído se guarda en este navegador
const CLAVE_LEIDOS = "avisos.recolecciones.leidos";
const MAXIMO_LEIDOS = 200;

// Si el recordatorio cambia (se programó la recolección o ya venció), se vuelve a avisar
export const claveRecoleccion = (r: RecoleccionProxima) =>
  `${r.rentaId}-${r.vencida ? "vencida" : "proxima"}-${r.trasladoFolio ?? "sin-horario"}`;

function leerLeidos(): string[] {
  try {
    const lista: unknown = JSON.parse(localStorage.getItem(CLAVE_LEIDOS) ?? "[]");
    return Array.isArray(lista) ? lista.filter((c) => typeof c === "string") : [];
  } catch {
    return []; // Navegador sin almacenamiento (por ejemplo, ventana privada)
  }
}

/**
 * OHU-25: recordatorio de las recolecciones próximas. Usa la misma anticipación
 * que los apartados próximos ("Avisar con anticipación").
 */
export function useRecoleccionesProximas() {
  const [dias] = useAnticipacion();
  const [recolecciones, setRecolecciones] = useState<RecoleccionProxima[]>([]);
  const [leidos, setLeidos] = useState<string[]>(leerLeidos);

  useEffect(() => {
    let activo = true;
    const cargar = () =>
      listarRecoleccionesProximas(dias)
        .then((lista) => {
          if (activo) setRecolecciones(lista);
        })
        .catch(() => {});

    cargar();
    const intervalo = setInterval(cargar, INTERVALO_MS);
    return () => {
      activo = false;
      clearInterval(intervalo);
    };
  }, [dias]);

  const nuevas = useMemo(() => {
    const vistos = new Set(leidos);
    return new Set(recolecciones.filter((r) => !vistos.has(claveRecoleccion(r))).map(claveRecoleccion));
  }, [recolecciones, leidos]);

  const marcarComoLeidos = useCallback(() => {
    const todos = [...new Set([...leidos, ...recolecciones.map(claveRecoleccion)])].slice(-MAXIMO_LEIDOS);
    setLeidos(todos);
    try {
      localStorage.setItem(CLAVE_LEIDOS, JSON.stringify(todos));
    } catch {
      // Si no se puede guardar, solo dura mientras la pantalla esté abierta
    }
  }, [leidos, recolecciones]);

  return { recolecciones, nuevas, noLeidos: nuevas.size, marcarComoLeidos };
}