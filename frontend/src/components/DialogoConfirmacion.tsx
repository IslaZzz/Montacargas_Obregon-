import { useEffect, useId, useRef } from "react";
import "../styles/dialogo.css";

interface DialogoConfirmacionProps {
  abierto: boolean;
  titulo: string;
  puntos?: string[]; // Lo que pasa al confirmar; si no hay, no se muestra la lista
  textoConfirmar: string;
  iconoConfirmar?: string; // Nombre del ícono en Material Symbols
  textoCancelar?: string;
  peligro?: boolean; // Botón de confirmar en rojo
  ocupado?: boolean; // Deshabilita los botones mientras se guarda
  onConfirmar: () => void;
  onCancelar: () => void;
}

// Ventana para confirmar una acción. Usa <dialog>: bloquea la página de atrás y se cierra con Esc.
export default function DialogoConfirmacion({
  abierto,
  titulo,
  puntos,
  textoConfirmar,
  iconoConfirmar,
  textoCancelar = "No, regresar",
  peligro,
  ocupado,
  onConfirmar,
  onCancelar,
}: DialogoConfirmacionProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const idTitulo = useId();

  // Abre o cierra la ventana según "abierto"
  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (abierto && !dialogo.open) dialogo.showModal();
    if (!abierto && dialogo.open) dialogo.close();
  }, [abierto]);

  return (
    <dialog
      ref={ref}
      className="dialogo"
      aria-labelledby={idTitulo}
      onCancel={(e) => {
        // Esc: se cierra desde el estado, igual que con "No, regresar"
        e.preventDefault();
        if (!ocupado) onCancelar();
      }}
    >
      <div className="dialogo-contenido">
        <h2 id={idTitulo}>{titulo}</h2>

        {puntos && puntos.length > 0 && (
          <>
            <p className="dialogo-subtitulo">Al confirmar:</p>
            <ul className="dialogo-puntos">
              {puntos.map((punto) => (
                <li key={punto}>
                  <span className="material-symbols-rounded" aria-hidden="true">
                    check
                  </span>
                  {punto}
                </li>
              ))}
            </ul>
          </>
        )}

        <div className="dialogo-botones">
          <button
            type="button"
            className="btn btn-tertiary"
            disabled={ocupado}
            onClick={onCancelar}
          >
            {textoCancelar}
          </button>
          <button
            type="button"
            className={`btn ${peligro ? "btn-peligro" : "btn-primary"}`}
            disabled={ocupado}
            onClick={onConfirmar}
          >
            {iconoConfirmar && (
              <span className="material-symbols-rounded" aria-hidden="true">
                {iconoConfirmar}
              </span>
            )}
            {textoConfirmar}
          </button>
        </div>
      </div>
    </dialog>
  );
}
