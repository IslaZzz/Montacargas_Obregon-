import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import FilterRow from "../../components/filters/filterRow";
import { useTitulo } from "../../hooks/useTitulo";
import { aFecha, formatoDia } from "../../utils/formato";
import CalendarioOperaciones from "./CalendarioOperaciones";
import { obtenerInicio } from "./dashboardApi";
import type { Aviso, Inicio, Operacion, TipoAviso, TipoOperacion } from "./dashboardTypes";
import { claveDia, etiquetaDia, formatoHora } from "./dashboardUtils";
import "../../styles/dashboard/dashboard.css";

// Cuántas operaciones se muestran en el panel antes del calendario
const PROXIMAS_VISIBLES = 3;

// Cada operación pertenece a uno de los filtros
const GRUPO: Record<TipoOperacion, string> = {
  ENTREGA: "TRASLADOS",
  RECOLECCION: "TRASLADOS",
  REVISION: "REVISIONES",
  MANTENIMIENTO: "MANTENIMIENTOS",
};

// Opciones de "Filtrar"; "all" muestra todas
const FILTROS = [
  { label: "Traslados", value: "TRASLADOS" },
  { label: "Revisiones", value: "REVISIONES" },
  { label: "Mantenimientos", value: "MANTENIMIENTOS" },
];

const SIN_PROXIMAS: Record<string, string> = {
  all: "No hay operaciones próximas.",
  TRASLADOS: "No hay traslados próximos.",
  REVISIONES: "No hay revisiones próximas.",
  MANTENIMIENTOS: "No hay mantenimientos con fecha próxima.",
};

// Texto del botón de los avisos que no llevan a una renta
const ACCION_AVISO: Record<TipoAviso, string> = {
  TRASLADO_ATRASADO: "Ir a traslados",
  TRASLADO_SIN_OPERADOR: "Asignar",
  ENTREGA_EN_RIESGO: "Ver renta",
  MANTENIMIENTO_VENCIDO: "Ir a mantenimientos",
  FALTA_RECOLECCION: "Ver renta",
};

// Botón chico con flecha
function BotonVer({ texto, onClick }: { texto: string; onClick: () => void }) {
  return (
    <button type="button" className="inicio-btn-ver" onClick={onClick}>
      {texto}
      <span className="material-symbols-rounded" aria-hidden="true">
        arrow_forward
      </span>
    </button>
  );
}

// Algo que el auxiliar tiene que resolver
function TarjetaAviso({ aviso, onIr }: { aviso: Aviso; onIr: (ruta: string) => void }) {
  const destino = aviso.rentaId !== null ? `/rentas/${aviso.rentaId}` : aviso.ruta ?? "/";
  return (
    <div className="inicio-alerta">
      <div className="inicio-alerta-texto">
        <h3>{aviso.titulo}</h3>
        <p>{aviso.detalle}</p>
      </div>
      <BotonVer texto={ACCION_AVISO[aviso.tipo]} onClick={() => onIr(destino)} />
    </div>
  );
}

// Tarjeta con el número de pendientes del día
function TarjetaPendiente({
  numero,
  texto,
  accion,
  onClick,
}: {
  numero: number | null;
  texto: string;
  accion: string;
  onClick: () => void;
}) {
  return (
    <div className="inicio-pendiente">
      <div className="inicio-pendiente-cuerpo">
        <span className="inicio-pendiente-numero">{numero ?? "–"}</span>
        <span className="inicio-pendiente-texto">{texto}</span>
      </div>
      <button type="button" className="inicio-pendiente-ver" onClick={onClick}>
        {accion}
        <span className="material-symbols-rounded" aria-hidden="true">
          arrow_forward
        </span>
      </button>
    </div>
  );
}

// "10:00-12:00" para traslados; "09:00" para revisiones y mantenimientos
const horario = (o: Operacion) =>
  o.hasta ? `${formatoHora(o.desde)}-${formatoHora(o.hasta)}` : formatoHora(o.desde);

// Inicio: lo que hay que atender hoy y las próximas operaciones
export default function Dashboard() {
  useTitulo("Inicio");
  const navigate = useNavigate();
  // La fecha se toma una sola vez, al abrir la pantalla
  const [hoy] = useState(() => new Date());
  const [inicio, setInicio] = useState<Inicio | null>(null);
  const [error, setError] = useState(false);
  const [filtroAbierto, setFiltroAbierto] = useState(false);
  const [filtro, setFiltro] = useState("all");

  // Al cerrar el filtro se vuelven a ver todas
  const alternarFiltro = () => {
    if (filtroAbierto) setFiltro("all");
    setFiltroAbierto(!filtroAbierto);
  };

  useEffect(() => {
    let activo = true;
    obtenerInicio()
      .then((datos) => activo && setInicio(datos))
      .catch(() => activo && setError(true));
    return () => {
      activo = false;
    };
  }, []);

  const pendientes = inicio?.pendientes ?? null;
  const avisos = inicio?.avisos ?? [];
  const operaciones = inicio?.operaciones ?? [];

  const proximas = operaciones
    .filter((o) => filtro === "all" || GRUPO[o.tipo] === filtro)
    .slice(0, PROXIMAS_VISIBLES);

  const diasConOperacion = new Set(operaciones.map((o) => claveDia(aFecha(o.desde))));

  return (
    <div className="inicio">
      <div className="inicio-principal">
        <h1 className="inicio-fecha">{formatoDia(hoy)}.</h1>

        {error && (
          <p className="inicio-error" role="alert">
            No se pudo cargar la información de hoy. Revisa que el servidor esté encendido.
          </p>
        )}

        {avisos.length > 0 && (
          <section className="inicio-seccion" aria-labelledby="titulo-atencion">
            <h2 id="titulo-atencion" className="inicio-titulo inicio-titulo-alerta">
              <span className="material-symbols-rounded" aria-hidden="true">
                emergency_home
              </span>
              Requiere atención
            </h2>
            <div className="inicio-alertas">
              {avisos.map((a, i) => (
                <TarjetaAviso key={`${a.tipo}-${i}`} aviso={a} onIr={navigate} />
              ))}
            </div>
          </section>
        )}

        <section className="inicio-seccion" aria-labelledby="titulo-pendientes">
          <h2 id="titulo-pendientes" className="inicio-titulo">
            Pendientes hoy
          </h2>
          <div className="inicio-pendientes">
            <TarjetaPendiente
              numero={pendientes?.entregas ?? null}
              texto="Entregas"
              accion="Ver entregas"
              onClick={() => navigate("/traslados")}
            />
            <TarjetaPendiente
              numero={pendientes?.recolecciones ?? null}
              texto="Recolecciones"
              accion="Ver recolecciones"
              onClick={() => navigate("/traslados")}
            />
            <TarjetaPendiente
              numero={pendientes?.revisiones ?? null}
              texto="Revisiones"
              accion="Ver revisiones"
              onClick={() => navigate("/revisiones")}
            />
            <TarjetaPendiente
              numero={pendientes?.mantenimientos ?? null}
              texto="Mantenimientos"
              accion="Ver mantenimientos"
              onClick={() => navigate("/mantenimientos")}
            />
          </div>
        </section>
      </div>

      <aside className="inicio-panel" aria-labelledby="titulo-proximas">
        <div className="inicio-panel-encabezado">
          <h2 id="titulo-proximas">Próximas operaciones</h2>
          <button
            type="button"
            className="inicio-filtrar"
            aria-expanded={filtroAbierto}
            aria-controls="filtro-operaciones"
            onClick={alternarFiltro}
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              filter_alt
            </span>
            Filtrar
          </button>
        </div>

        {filtroAbierto && (
          <div id="filtro-operaciones" className="inicio-filtro">
            <FilterRow options={FILTROS} selectedValue={filtro} allLabel="Todas" onChange={setFiltro} />
          </div>
        )}

        {inicio && proximas.length === 0 && <p className="inicio-sin-operaciones">{SIN_PROXIMAS[filtro]}</p>}
        <ul className="inicio-operaciones">
          {proximas.map((o, i) => (
            <li key={`${o.tipo}-${o.desde}-${i}`}>
              <span className={`inicio-punto inicio-punto-${o.tipo.toLowerCase()}`} aria-hidden="true" />
              <div className="inicio-operacion-texto">
                <strong>
                  {o.titulo} <span>{etiquetaDia(aFecha(o.desde), hoy)}</span>
                </strong>
                <span>
                  {o.detalle} · <span className="inicio-horario">{horario(o)}</span>
                </span>
              </div>
              <BotonVer
                texto={o.rentaId !== null ? "Ver renta" : "Ver"}
                onClick={() => navigate(o.rentaId !== null ? `/rentas/${o.rentaId}` : o.ruta)}
              />
            </li>
          ))}
        </ul>

        <section className="inicio-calendario" aria-labelledby="titulo-calendario">
          <h2 id="titulo-calendario">
            <span className="material-symbols-rounded" aria-hidden="true">
              calendar_clock
            </span>
            Calendario de operaciones
          </h2>
          <CalendarioOperaciones hoy={hoy} diasConOperacion={diasConOperacion} />
        </section>
      </aside>
    </div>
  );
}