import { useEffect, useRef, useState } from "react";
import { ANTICIPACIONES, textoAnticipacion } from "./avisosUtils";
import { useAnticipacion } from "./useApartadosProximos";

// "Avisar con anticipación": con cuántos días antes del inicio aparece el aviso
export default function SelectorAnticipacion() {
  const [dias, cambiar] = useAnticipacion();
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef<HTMLDivElement>(null);

  // Se cierra al tocar fuera o con Escape
  useEffect(() => {
    if (!abierto) return;
    const alTocar = (e: MouseEvent) => {
      if (!contenedor.current?.contains(e.target as Node)) setAbierto(false);
    };
    const alTeclear = (e: KeyboardEvent) => {
      if (e.key === "Escape") setAbierto(false);
    };
    document.addEventListener("mousedown", alTocar);
    document.addEventListener("keydown", alTeclear);
    return () => {
      document.removeEventListener("mousedown", alTocar);
      document.removeEventListener("keydown", alTeclear);
    };
  }, [abierto]);

  const elegir = (opcion: number) => {
    cambiar(opcion);
    setAbierto(false);
  };

  return (
    <div className="anticipacion" ref={contenedor}>
      <span id="anticipacion-etiqueta" className="anticipacion-etiqueta">
        Avisar con anticipación
      </span>
      <button
        type="button"
        className="anticipacion-boton"
        aria-haspopup="listbox"
        aria-expanded={abierto}
        aria-labelledby="anticipacion-etiqueta anticipacion-valor"
        onClick={() => setAbierto(!abierto)}
      >
        <span className="material-symbols-rounded" aria-hidden="true">
          notifications_active
        </span>
        <span id="anticipacion-valor" className="anticipacion-valor">
          {textoAnticipacion(dias)} del inicio
        </span>
        <span className="material-symbols-rounded" aria-hidden="true">
          arrow_drop_down
        </span>
      </button>

      {abierto && (
        <ul className="anticipacion-opciones" role="listbox" aria-labelledby="anticipacion-etiqueta">
          {ANTICIPACIONES.map((opcion) => (
            <li key={opcion} role="option" aria-selected={opcion === dias}>
              <button type="button" onClick={() => elegir(opcion)}>
                {textoAnticipacion(opcion)}
                {opcion === dias && (
                  <span className="material-symbols-rounded" aria-hidden="true">
                    check
                  </span>
                )}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
