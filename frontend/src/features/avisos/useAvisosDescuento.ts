import { useCallback, useEffect, useState } from "react";
import {
  listarAvisosDescuento,
  marcarAvisoDescuentoLeido,
  marcarAvisosDescuentoLeidos,
} from "./avisosApi";
import type { AvisoDescuento } from "./avisosTypes";

// Cada cuánto se vuelven a pedir los avisos
const INTERVALO_MS = 60_000;

// Las respuestas de gerencia a los descuentos del usuario; se actualizan solas cada minuto
export function useAvisosDescuento() {
  const [avisos, setAvisos] = useState<AvisoDescuento[] | null>(null);

  useEffect(() => {
    let activo = true;
    const cargar = () =>
      listarAvisosDescuento()
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

  // Marca todos los avisos como leídos
  const marcarComoLeidos = useCallback(() => {
    marcarAvisosDescuentoLeidos()
      .then(setAvisos)
      .catch(() => {});
  }, []);

  // Marca un solo aviso como leído, por ejemplo al abrir su cotización
  const marcarLeido = useCallback((id: number) => {
    marcarAvisoDescuentoLeido(id)
      .then(setAvisos)
      .catch(() => {});
  }, []);

  return { avisos: avisos ?? [], noLeidos, marcarComoLeidos, marcarLeido };
}