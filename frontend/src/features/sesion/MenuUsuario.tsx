import { useEffect, useRef, useState } from "react";
import { puesto, rolCorto } from "./sesionUtils";
import { useSesion } from "./useSesion";
import "../../styles/sesion/menu-usuario.css";

// Botón del encabezado con el nombre y el rol, y su menú con «Cerrar sesión» (HU-28)
export default function MenuUsuario() {
  const { sesion, salir } = useSesion();
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

  if (!sesion) return null;
  const texto = `${sesion.nombre} · ${rolCorto(sesion.rol)}`;

  return (
    <div className="usuario-contenedor" ref={contenedor}>
      <button
        type="button"
        className="encabezado-btn usuario-btn"
        aria-label={texto}
        aria-expanded={abierto}
        aria-controls="menu-usuario"
        onClick={() => setAbierto(!abierto)}
      >
        <span className="material-symbols-rounded" aria-hidden="true">
          account_circle
        </span>
        <span className="encabezado-texto">{texto}</span>
      </button>

      {abierto && (
        <div id="menu-usuario" className="usuario-menu">
          <div className="usuario-datos">
            <strong>{sesion.nombre}</strong>
            <span className="usuario-rol">
              <span className="material-symbols-rounded" aria-hidden="true">
                badge
              </span>
              Rol: {puesto(sesion.rol)}
            </span>
            <span className="usuario-correo">{sesion.correo}</span>
          </div>
          <button type="button" className="usuario-salir" onClick={salir}>
            <span className="material-symbols-rounded" aria-hidden="true">
              logout
            </span>
            Cerrar sesión
          </button>
        </div>
      )}
    </div>
  );
}
