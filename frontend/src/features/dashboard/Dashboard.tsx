import { useEffect, useState } from "react";
import FilterRow from "../../components/filters/filterRow";
import { useTitulo } from "../../hooks/useTitulo";
import { aFecha, formatoDia } from "../../utils/formato";
import CalendarioOperaciones from "./CalendarioOperaciones";
import { contarPreparacionesDeHoy, listarOperaciones } from "./dashboardApi";
import type { Operacion } from "./dashboardTypes";
import { claveDia, diasEntre, etiquetaDia, formatoHora, mismoDia } from "./dashboardUtils";
import "../../styles/dashboard/dashboard.css";

// Cuántas operaciones caben en el panel antes del calendario
const PROXIMAS_VISIBLES = 4;

const NOMBRE_TIPO = { ENTREGA: "Entrega", RECOLECCION: "Recolección" } as const;

// Opciones de "Filtrar"; "all" muestra las dos
const FILTROS = [
  { label: "Entregas", value: "ENTREGA" },
  { label: "Recolecciones", value: "RECOLECCION" },
];

const SIN_PROXIMAS: Record<string, string> = {
  all: "No hay operaciones próximas.",
  ENTREGA: "No hay entregas próximas.",
  RECOLECCION: "No hay recolecciones próximas.",
};

// Botón chico "Ver renta" (sin acción hasta que exista la pantalla de la renta)
function BotonVerRenta({ folio }: { folio: string }) {
  return (
    // TODO: abrir el detalle de la renta cuando exista su pantalla
    <button
      type="button"
      className="inicio-btn-ver"
      aria-disabled="true"
      aria-label={`Ver renta ${folio}`}
    >
      Ver renta
      <span className="material-symbols-rounded" aria-hidden="true">
        arrow_forward
      </span>
    </button>
  );
}

// Entrega de hoy a la que todavía le faltan equipos por preparar
function TarjetaAtencion({ operacion }: { operacion: Operacion }) {
  const faltan = operacion.equiposPorPreparar;
  return (
    <div className="inicio-alerta">
      <h3>
        {NOMBRE_TIPO[operacion.tipo]} hoy - Renta {operacion.folioRenta}
      </h3>
      <ul>
        <li>
          {operacion.equipos} {operacion.equipos === 1 ? "equipo" : "equipos"}{" "}
          {formatoHora(operacion.desde)}–{formatoHora(operacion.hasta)}
        </li>
        <li className="inicio-alerta-faltan">
          {faltan === 1 ? "Falta 1 equipo" : `Faltan ${faltan} equipos`} por preparar
        </li>
      </ul>
      <BotonVerRenta folio={operacion.folioRenta} />
    </div>
  );
}

// Tarjeta con el número de pendientes del día
function TarjetaPendiente({
  numero,
  texto,
  accion,
}: {
  numero: number | null;
  texto: string;
  accion: string;
}) {
  return (
    <div className="inicio-pendiente">
      <div className="inicio-pendiente-cuerpo">
        <span className="inicio-pendiente-numero">{numero ?? "–"}</span>
        <span className="inicio-pendiente-texto">{texto}</span>
      </div>
      {/* TODO: llevar a su pantalla cuando exista */}
      <button type="button" className="inicio-pendiente-ver" aria-disabled="true">
        {accion}
        <span className="material-symbols-rounded" aria-hidden="true">
          arrow_forward
        </span>
      </button>
    </div>
  );
}

// Inicio: lo que hay que atender hoy y las próximas entregas y recolecciones
export default function Dashboard() {
  useTitulo("Inicio");
  // La fecha se toma una sola vez, al abrir la pantalla
  const [hoy] = useState(() => new Date());
  const [operaciones, setOperaciones] = useState<Operacion[]>([]);
  const [preparaciones, setPreparaciones] = useState<number | null>(null);
  const [filtroAbierto, setFiltroAbierto] = useState(false);
  const [filtro, setFiltro] = useState("all");

  // Al cerrar el filtro se vuelven a ver todas
  const alternarFiltro = () => {
    if (filtroAbierto) setFiltro("all");
    setFiltroAbierto(!filtroAbierto);
  };

  useEffect(() => {
    listarOperaciones(hoy).then(setOperaciones);
    // Si falla, la tarjeta muestra "–"
    contarPreparacionesDeHoy(hoy)
      .then(setPreparaciones)
      .catch(() => setPreparaciones(null));
  }, [hoy]);

  const deHoy = operaciones.filter((o) => mismoDia(aFecha(o.desde), hoy));
  const entregasHoy = deHoy.filter((o) => o.tipo === "ENTREGA");
  const recoleccionesHoy = deHoy.filter((o) => o.tipo === "RECOLECCION");
  // Operaciones de hoy con problemas: por ahora, equipos que faltan por preparar
  const requierenAtencion = deHoy.filter((o) => o.equiposPorPreparar > 0);

  const proximas = operaciones
    .filter((o) => diasEntre(hoy, aFecha(o.desde)) > 0)
    .filter((o) => filtro === "all" || o.tipo === filtro)
    .sort((a, b) => a.desde.localeCompare(b.desde))
    .slice(0, PROXIMAS_VISIBLES);

  const diasConOperacion = new Set(operaciones.map((o) => claveDia(aFecha(o.desde))));

  return (
    <div className="inicio">
      <div className="inicio-principal">
        <h1 className="inicio-fecha">{formatoDia(hoy)}.</h1>

        {requierenAtencion.length > 0 && (
          <section className="inicio-seccion" aria-labelledby="titulo-atencion">
            <h2 id="titulo-atencion" className="inicio-titulo inicio-titulo-alerta">
              <span className="material-symbols-rounded" aria-hidden="true">
                emergency_home
              </span>
              Requiere atención
            </h2>
            {requierenAtencion.map((o) => (
              <TarjetaAtencion key={`${o.rentaId}-${o.tipo}`} operacion={o} />
            ))}
          </section>
        )}

        <section className="inicio-seccion" aria-labelledby="titulo-pendientes">
          <h2 id="titulo-pendientes" className="inicio-titulo">
            Pendientes hoy
          </h2>
          <div className="inicio-pendientes">
            <TarjetaPendiente
              numero={entregasHoy.length}
              texto="Entregas"
              accion="Ver entregas"
            />
            <TarjetaPendiente
              numero={recoleccionesHoy.length}
              texto="Recolecciones"
              accion="Ver recolecciones"
            />
            <TarjetaPendiente
              numero={preparaciones}
              texto="Preparaciones"
              accion="Ver preparaciones"
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
            <FilterRow
              options={FILTROS}
              selectedValue={filtro}
              allLabel="Todas"
              onChange={setFiltro}
            />
          </div>
        )}

        {proximas.length === 0 && (
          <p className="inicio-sin-operaciones">{SIN_PROXIMAS[filtro]}</p>
        )}
        <ul className="inicio-operaciones">
          {proximas.map((o) => (
            <li key={`${o.rentaId}-${o.tipo}`}>
              <span
                className={`inicio-punto inicio-punto-${o.tipo.toLowerCase()}`}
                aria-hidden="true"
              />
              <div className="inicio-operacion-texto">
                <strong>
                  {NOMBRE_TIPO[o.tipo]}{" "}
                  <span>{etiquetaDia(aFecha(o.desde), hoy)}</span>
                </strong>
                <span>
                  Folio {o.folioRenta} -{" "}
                  <span className="inicio-horario">
                    {formatoHora(o.desde)}-{formatoHora(o.hasta)}
                  </span>
                </span>
              </div>
              <BotonVerRenta folio={o.folioRenta} />
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
