import { useCallback, useEffect, useState } from "react";
import {
  listarAvisosOperacion,
  marcarAvisoOperacionLeido,
  marcarAvisosOperacionLeidos,
} from "./avisosApi";
import type { AvisoOperacion } from "./avisosTypes";

// Cada cuánto se vuelven a pedir los avisos
const INTERVALO_MS = 60_000;

// Los avisos de los traslados para el usuario; se actualizan solos cada minuto
export function useAvisosOperacion() {
  const [avisos, setAvisos] = useState<AvisoOperacion[] | null>(null);

  useEffect(() => {
    let activo = true;
    const cargar = () =>
      listarAvisosOperacion()
        .then((lista) => {
          if (activo) setAvisos(lista);
        })
        .catch(() => {});

    cargar();
    const intervalo = setInterval(cargar, INTERVALO_MS);
    return () => {
      activo = false;
      clearInterval(intervalo);
    };
  }, []);

  const noLeidos = (avisos ?? []).filter((a) => !a.leido).length;

  const marcarComoLeidos = useCallback(() => {
    marcarAvisosOperacionLeidos()
      .then(setAvisos)
      .catch(() => {});
  }, []);

  // Al abrir la renta o la revisión desde el aviso
  const marcarLeido = useCallback((id: number) => {
    marcarAvisoOperacionLeido(id)
      .then(setAvisos)
      .catch(() => {});
  }, []);

  return { avisos: avisos ?? [], noLeidos, marcarComoLeidos, marcarLeido };
}