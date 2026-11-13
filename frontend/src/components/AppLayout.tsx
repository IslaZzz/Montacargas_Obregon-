import { Outlet } from "react-router-dom";
import MenuLateral from "./MenuLateral";
import BotonAvisos from "../features/avisos/BotonAvisos";
import MenuUsuario from "../features/sesion/MenuUsuario";
import { useSesion } from "../features/sesion/useSesion";
import { puesto, rolMenu } from "../features/sesion/sesionUtils";
import { useState } from "react";

export default function AppLayout() {
  // Menú abierto o cerrado, solo importa en celular
  const [menuAbierto, setMenuAbierto] = useState(false);
  const { sesion, salir } = useSesion();

  // RutaProtegida ya revisó que haya sesión
  if (!sesion) return null;

  return (
    <div className="app">
      {/* Usuario con la sesión abierta (HU-28) */}
      <MenuLateral
        rol={rolMenu(sesion.rol)}
        nombreUsuario={sesion.nombre}
        puesto={puesto(sesion.rol)}
        abierto={menuAbierto}
        onCerrar={() => setMenuAbierto(false)}
        onCerrarSesion={salir}
      />

      {/* Fondo oscuro que cierra el menú al tocarlo */}
      {menuAbierto && (
        <div className="menu-fondo" onClick={() => setMenuAbierto(false)} />
      )}

      <div className="principal">
        <header className="encabezado">
          {/* Botón de hamburguesa, solo se ve en celular */}
          <button
            type="button"
            className="btn-menu"
            aria-label="Abrir menú"
            onClick={() => setMenuAbierto(true)}
          >
            <span className="material-symbols-rounded">menu</span>
          </button>

          <label className="buscador">
            <input placeholder="Busca un folio, cliente o equipo" />
            <span className="material-symbols-rounded">search</span>
          </label>
          <span className="espacio" />
          <BotonAvisos />
          <MenuUsuario />
        </header>

        <Outlet />
      </div>
    </div>
  );
}
