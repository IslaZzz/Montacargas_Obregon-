import type { ReactNode } from "react";
import BarraSuperior from "./BarraSuperior";
import Boton from "./Boton";
import Icono from "./Icono";
import Pantalla from "./Pantalla";

interface Props {
  tipo: "exito" | "error";
  titulo: string; // "Traslado iniciado", "Falla reportada"
  texto?: string;
  icono?: string; // Por defecto: palomita en éxito, campana en error
  children?: ReactNode; // Resumen opcional debajo del texto
  accion: { texto: string; onClick: () => void; icono?: string };
}

/** Pantalla de cierre después de enviar algo. */
export default function Resultado({ tipo, titulo, texto, icono, children, accion }: Props) {
  return (
    <Pantalla barraSuperior={<BarraSuperior />}>
      <div className="resultado">
        <span className={`resultado-icono resultado-${tipo}`}>
          <Icono nombre={icono ?? (tipo === "exito" ? "check" : "notifications_active")} />
        </span>
        <h2>{titulo}</h2>
        {texto && <p>{texto}</p>}
        {children}
        <Boton icono={accion.icono ?? "arrow_back"} onClick={accion.onClick}>
          {accion.texto}
        </Boton>
      </div>
    </Pantalla>
  );
}