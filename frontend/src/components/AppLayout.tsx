import { Outlet } from "react-router-dom";
import MenuLateral from "./MenuLateral";
import { useState } from "react";

export default function AppLayout() {
  // Menú abierto o cerrado, solo importa en celular
  const [menuAbierto, setMenuAbierto] = useState(false);

  return (
    <div className="app">
      {/* Cambiar esto cuando se implemente la autenticación de usuarios */}
      <MenuLateral
        rol="auxiliar"
        nombreUsuario="Juan Pérez"
        puesto="Auxiliar de oficina"
        abierto={menuAbierto}
        onCerrar={() => setMenuAbierto(false)}
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
          <button type="button" className="encabezado-btn">
            <span className="material-symbols-rounded">notifications</span>
            <span className="encabezado-texto">Avisos</span>
          </button>
          <button type="button" className="encabezado-btn">
            <span className="material-symbols-rounded">account_circle</span>
            <span className="encabezado-texto">Mi cuenta</span>
          </button>
        </header>

        <Outlet />
      </div>
    </div>
  );
}
