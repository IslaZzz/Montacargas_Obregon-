import { createContext } from "react";
import type { DatosInicioSesion, Sesion } from "./sesionTypes";

export interface ValorSesion {
  // undefined: todavía se está revisando; null: no hay sesión
  sesion: Sesion | null | undefined;
  // true si el usuario cerró su sesión (no venció)
  cerroSesion: boolean;
  // Responde false si el correo o la contraseña no coinciden
  entrar: (datos: DatosInicioSesion) => Promise<boolean>;
  salir: () => Promise<void>;
}

export const SesionContexto = createContext<ValorSesion | null>(null);
