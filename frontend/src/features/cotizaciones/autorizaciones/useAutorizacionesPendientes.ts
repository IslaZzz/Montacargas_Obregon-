import { useEffect, useState } from "react";
import { contarAutorizacionesPendientes, EVENTO_AUTORIZACIONES } from "./autorizacionesApi";

// Total de solicitudes pendientes para el contador del menú. Solo se consulta si el usuario es de gerencia
export function useAutorizacionesPendientes(activo: boolean) {
  const [total, setTotal] = useState(0);

  useEffect(() => {
    if (!activo) return;

    let vigente = true;
    const cargar = () =>
      contarAutorizacionesPendientes()
        .then((n) => vigente && setTotal(n))
        .catch(() => {});

    cargar();
    // Se vuelve a consultar cuando se autoriza o se rechaza una solicitud
    window.addEventListener(EVENTO_AUTORIZACIONES, cargar);

    return () => {
      vigente = false;
      window.removeEventListener(EVENTO_AUTORIZACIONES, cargar);
    };
  }, [activo]);

  return total;
}