import type { ButtonHTMLAttributes } from "react";
import Icono from "./Icono";

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: "primario" | "secundario" | "peligro";
  icono?: string;
  bloqueado?: boolean; // Deshabilitado con candado: "Iniciar traslado" cuando aún no se puede
  cargando?: boolean; // Deshabilitado mientras se guarda
}

/** Botón grande, a lo ancho. */
export default function Boton({ variante = "primario", icono, bloqueado, cargando, children, className = "", ...resto }: Props) {
  return (
    <button
      type="button"
      {...resto}
      disabled={resto.disabled || bloqueado || cargando}
      className={`boton boton-${variante} ${className}`}
    >
      {(bloqueado || icono) && <Icono nombre={bloqueado ? "lock" : icono!} />}
      {cargando ? "Guardando…" : children}
    </button>
  );
}