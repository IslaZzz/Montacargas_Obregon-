import { useContext } from "react";
import { SesionContexto } from "./sesionContexto";

// La sesión del usuario y las acciones para entrar y salir
export function useSesion() {
  const valor = useContext(SesionContexto);
  if (!valor) throw new Error("useSesion debe usarse dentro de SesionProvider");
  return valor;
}
