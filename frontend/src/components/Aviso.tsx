import type { ReactNode } from "react";

export type TipoAviso = "exito" | "error" | "advertencia";

interface AvisoProps {
  tipo: TipoAviso;
  titulo: string;
  // Texto de apoyo debajo del título
  children?: ReactNode;
  // Si se pasa, aparece la X para cerrarlo
  onCerrar?: () => void;
  // Otro ícono de Material Symbols en lugar del de su tipo
  icono?: string;
}

const iconos: Record<TipoAviso, string> = {
  exito: "check_circle",
  error: "error",
  advertencia: "warning",
};

// Franja de aviso dentro de la página, normalmente debajo del título
export default function Aviso({ tipo, titulo, children, onCerrar, icono }: AvisoProps) {
  return (
    <div
      className={`aviso aviso-tipo-${tipo}`}
      // Los errores se anuncian de inmediato a los lectores de pantalla
      role={tipo === "error" ? "alert" : "status"}
    >
      <span className="material-symbols-rounded aviso-icono">
        {icono ?? iconos[tipo]}
      </span>

      <div className="aviso-texto">
        <strong>{titulo}</strong>
        {children && <div className="aviso-detalle">{children}</div>}
      </div>

      {onCerrar && (
        <button
          type="button"
          className="aviso-cerrar"
          aria-label="Cerrar aviso"
          onClick={onCerrar}
        >
          <span className="material-symbols-rounded">close</span>
        </button>
      )}
    </div>
  );
}