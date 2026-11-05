import { useEffect } from "react";

// Cambia el título de la pestaña mientras la pantalla está abierta
export function useTitulo(titulo: string) {
  useEffect(() => {
    document.title = `${titulo} | Montacargas Obregón`;
  }, [titulo]);
}