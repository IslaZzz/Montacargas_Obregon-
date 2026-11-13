import { NavLink } from "react-router-dom";

type Rol = "auxiliar" | "gerencia";

interface Opcion {
  texto: string;
  ruta: string;
  icono: string; // Nombre del ícono en Material Symbols
  soloGerencia?: boolean;
}

interface MenuLateralProps {
  rol: Rol;
  nombreUsuario: string;
  puesto: string;
  abierto?: boolean; // Solo se usa en celular
  onCerrar?: () => void; // Cierra el menú al elegir una opción
  onCerrarSesion?: () => void;
}

// Opciones del menú en el orden del diseño
const opciones: Opcion[] = [
  { texto: "Inicio", ruta: "/dashboard", icono: "home" },
  { texto: "Rentas", ruta: "/rentas", icono: "calendar_month" },
  { texto: "Clientes", ruta: "/clientes", icono: "group" },
  { texto: "Equipos", ruta: "/equipos", icono: "local_shipping" },
  {
    texto: "Disponibilidad",
    ruta: "/disponibilidad",
    icono: "event_available",
  },
  { texto: "Cotizaciones", ruta: "/cotizaciones", icono: "description" },
  { texto: "Revisiones", ruta: "/revisiones", icono: "fact_check" },
  { texto: "Traslados", ruta: "/traslados", icono: "swap_horiz" },
  { texto: "Mantenimientos", ruta: "/mantenimientos", icono: "build" },
  { texto: "Historial de rentas", ruta: "/historial-rentas", icono: "history" },
  {
    texto: "Autorizaciones",
    ruta: "/autorizaciones",
    icono: "rule",
    soloGerencia: true,
  },
  {
    texto: "Ingresos",
    ruta: "/ingresos",
    icono: "attach_money",
    soloGerencia: true,
  },
  { texto: "Modelos", ruta: "/modelos", icono: "sell" },
];

export default function MenuLateral({
  rol,
  nombreUsuario,
  puesto,
  abierto,
  onCerrar,
  onCerrarSesion,
}: MenuLateralProps) {
  // Gerencia ve todas las opciones, auxiliar solo las generales
  const visibles = opciones.filter(
    (o) => !o.soloGerencia || rol === "gerencia",
  );

  return (
    <aside className={"menu" + (abierto ? " abierto" : "")}>
      {/* Marca */}
      <div className="menu-marca">
        <span className="menu-emoji" aria-hidden="true">
          🚜
        </span>
        <span className="menu-texto">Montacargas Obregón.</span>
      </div>

      {/* Opciones, la activa se marca según la ruta */}
      <nav className="menu-nav" aria-label="Navegación principal">
        {visibles.map((o) => (
          <NavLink
            key={o.ruta}
            to={o.ruta}
            title={o.texto}
            onClick={onCerrar}
            className={({ isActive }) =>
              "menu-link" + (isActive ? " activo" : "")
            }
          >
            <span className="material-symbols-rounded">{o.icono}</span>
            <span className="menu-texto">{o.texto}</span>
          </NavLink>
        ))}
      </nav>

      {/* Usuario */}
      <div className="menu-usuario">
        <span className="material-symbols-rounded">account_circle</span>
        <div className="menu-texto">
          <strong>{nombreUsuario}</strong>
          <span>{puesto}</span>
        </div>
      </div>

      {/* Cerrar sesión */}
      <button type="button" className="menu-link" onClick={onCerrarSesion}>
        <span className="material-symbols-rounded">logout</span>
        <span className="menu-texto">Cerrar sesión</span>
      </button>
    </aside>
  );
}
