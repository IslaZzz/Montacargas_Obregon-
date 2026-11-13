import type { ReactNode } from "react";
import Icono from "./Icono";

type TipoAviso = "error" | "advertencia" | "info" | "exito";

const ICONO: Record<TipoAviso, string> = {
  error: "priority_high",
  advertencia: "warning",
  info: "info",
  exito: "check",
};

/** Recuadro de aviso dentro de una pantalla: "Aún no puedes iniciar el traslado"… */
export default function Aviso({ tipo, titulo, children }: { tipo: TipoAviso; titulo: string; children?: ReactNode }) {
  return (
    <div className={`aviso aviso-${tipo}`} role={tipo === "error" ? "alert" : "status"}>
      <span className="aviso-icono">
        <Icono nombre={ICONO[tipo]} />
      </span>
      <div>
        <p className="aviso-titulo">{titulo}</p>
        {children && <div className="aviso-texto">{children}</div>}
      </div>
    </div>
  );
}