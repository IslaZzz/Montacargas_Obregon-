import type { ReactNode } from "react";
import Icono from "./Icono";

interface Progreso {
  texto: string; // "2 de 3 equipos listos", "Faltan 3 campos y 4 fotos"
  valor: number; // De 0 a 1
  completo?: boolean; // Pone el ícono de palomita
  error?: boolean; // En rojo: "1 falla"
}

/** Barra fija abajo con el avance y el botón principal de la pantalla. */
export default function BarraAccion({ progreso, children }: { progreso?: Progreso; children?: ReactNode }) {
  return (
    <div className="barra-accion">
      {progreso && (
        <div className={`progreso${progreso.error ? " progreso-error" : ""}`}>
          {(progreso.completo || progreso.error) && <Icono nombre={progreso.error ? "error" : "check_circle"} />}
          <span>{progreso.texto}</span>
          <span
            className="progreso-barra"
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(progreso.valor * 100)}
          >
            <span style={{ width: `${Math.min(1, Math.max(0, progreso.valor)) * 100}%` }} />
          </span>
        </div>
      )}
      {children}
    </div>
  );
}