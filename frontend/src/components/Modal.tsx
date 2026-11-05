import { useEffect, useId } from "react";
import type { ReactNode } from "react";

interface ModalProps {
  abierto: boolean;
  titulo: string;
  subtitulo?: string;
  onCerrar: () => void;
  // Botones de abajo, por ejemplo Cancelar y Guardar
  acciones?: ReactNode;
  children: ReactNode;
}

// Ventana sobre la pantalla. Se cierra con la X, con Esc o tocando el fondo.
export default function Modal({
  abierto,
  titulo,
  subtitulo,
  onCerrar,
  acciones,
  children,
}: ModalProps) {
  const idTitulo = useId();

  useEffect(() => {
    if (!abierto) return;

    // Cierra con la tecla Esc
    const alPresionar = (e: KeyboardEvent) => {
      if (e.key === "Escape") onCerrar();
    };
    document.addEventListener("keydown", alPresionar);

    // Evita que la página de atrás se desplace mientras está abierto
    const desplazamientoPrevio = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", alPresionar);
      document.body.style.overflow = desplazamientoPrevio;
    };
  }, [abierto, onCerrar]);

  if (!abierto) return null;

  return (
    <div
      className="modal-fondo"
      // Solo cierra si el clic fue en el fondo, no dentro de la ventana
      onClick={(e) => {
        if (e.target === e.currentTarget) onCerrar();
      }}
    >
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
      >
        <header className="modal-encabezado">
          <div>
            <h2 id={idTitulo} className="modal-titulo">
              {titulo}
            </h2>
            {subtitulo && <p className="modal-subtitulo">{subtitulo}</p>}
          </div>
          <button
            type="button"
            className="modal-cerrar"
            aria-label="Cerrar"
            onClick={onCerrar}
          >
            <span className="material-symbols-rounded">close</span>
          </button>
        </header>

        <div className="modal-cuerpo">{children}</div>

        {acciones && <footer className="modal-acciones">{acciones}</footer>}
      </div>
    </div>
  );
}