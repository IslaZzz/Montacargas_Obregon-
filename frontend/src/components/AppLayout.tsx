import { Outlet } from "react-router-dom";
import MenuLateral from "./MenuLateral";

export default function AppLayout() {
  return (
    <div className="app">
      {/* Cambiar esto cuando se implemente la autenticación de usuarios */}
      <MenuLateral
        rol="auxiliar"
        nombreUsuario="Juan Pérez"
        puesto="Auxiliar de oficina"
      />

      <div className="principal">
        <header className="encabezado">
          <label className="buscador">
            <input placeholder="Busca un folio, cliente o equipo" />
            <span className="material-symbols-rounded">search</span>
          </label>
          <span className="espacio" />
          <button type="button" className="encabezado-btn">
            <span className="material-symbols-rounded">notifications</span>{" "}
            Avisos
          </button>
          <button type="button" className="encabezado-btn">
            <span className="material-symbols-rounded">account_circle</span> Mi
            cuenta
          </button>
        </header>

        <Outlet />
      </div>
    </div>
  );
}