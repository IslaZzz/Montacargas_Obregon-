import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Chip from "../../components/chips";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import FilterRow from "../../components/filters/filterRow";
import { useTitulo } from "../../hooks/useTitulo";
import { formatoFecha, formatoMoneda } from "../../utils/formato";
import { listarCotizaciones } from "./cotizacionesApi";
import type { CotizacionResumen } from "./cotizacionesTypes";
import { ESTADOS, chipDeEstado } from "./cotizacionesUtils";
import "../../styles/cotizaciones/cotizaciones.css";

// Columnas de la tabla en laptop
const columnas: TableColumn<CotizacionResumen>[] = [
  { header: "Folio", accessor: (c) => c.folio, className: "celda-folio" },
  {
    header: "Cliente",
    accessor: (c) => (
      <>
        <span className="celda-cliente">{c.cliente}</span>
        {c.empresa && <span className="celda-empresa">{c.empresa}</span>}
      </>
    ),
  },
  { header: "Creada", accessor: (c) => formatoFecha(c.fechaCreacion) },
  {
    header: "Total",
    accessor: (c) => formatoMoneda(c.total),
    className: "celda-total",
  },
  {
    header: "Estado",
    accessor: (c) => (
      <Chip category="cotizacion" status={chipDeEstado(c.estado)} />
    ),
  },
  {
    header: "Acciones",
    accessor: (c) => <BotonVer cotizacion={c} />,
    className: "celda-acciones",
  },
];

function BotonVer({ cotizacion }: { cotizacion: CotizacionResumen }) {
  return (
    <Link
      to={`/cotizaciones/${cotizacion.id}`}
      className="btn btn-tertiary"
      aria-label={`Ver ${cotizacion.folio}`}
    >
      Ver
    </Link>
  );
}

export default function VerCotizaciones() {
  useTitulo("Cotizaciones");

  const [cotizaciones, setCotizaciones] = useState<CotizacionResumen[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  // "all" muestra todas; si no, el estado elegido en el filtro
  const [estado, setEstado] = useState("all");

  useEffect(() => {
    // Se ejecuta una vez al abrir la pantalla
    listarCotizaciones()
      .then(setCotizaciones)
      .catch(() => setError("No fue posible cargar las cotizaciones."))
      .finally(() => setCargando(false));
  }, []);

  const visibles =
    estado === "all"
      ? cotizaciones
      : cotizaciones.filter((c) => c.estado === estado);

  // Un chip por estado, con cuántas cotizaciones tiene
  const filtros = ESTADOS.map((e) => ({
    label: e.etiqueta,
    value: e.valor,
    count: cotizaciones.filter((c) => c.estado === e.valor).length,
  }));

  const mensajeVacio = cargando
    ? "Cargando cotizaciones…"
    : "No hay cotizaciones en este estado.";

  return (
    <div className="pagina cotizaciones">
      {/* Título y botón */}
      <div className="cotizaciones-titulo">
        <div>
          <h1>Cotizaciones</h1>
          <p>Todas las cotizaciones y en qué estado va cada una.</p>
        </div>
        {/* La pantalla "Nueva cotización" es de HU-12 (Crear cotización); la ruta se registra cuando se implemente */}
        <Link to="/cotizaciones/nueva" className="btn btn-primary btn-grande">
          <span className="material-symbols-rounded" aria-hidden="true">
            add
          </span>
          Nueva cotización
        </Link>
      </div>

      {/* Filtro por estado */}
      <div className="cotizaciones-filtro">
        <span className="cotizaciones-filtro-etiqueta">Estado:</span>
        <FilterRow
          options={filtros}
          selectedValue={estado}
          allLabel="Todas"
          allCount={cotizaciones.length}
          onChange={setEstado}
        />
      </div>

      {error && (
        <div className="aviso-error" role="alert">
          <span className="material-symbols-rounded">error</span>
          <div>
            <strong>{error}</strong>
            <p>Revisa tu conexión y vuelve a abrir la pantalla.</p>
          </div>
        </div>
      )}

      {/* Laptop: tabla */}
      <div className="cotizaciones-tabla">
        <DataTable
          columns={columnas}
          data={visibles}
          getRowKey={(c) => c.id}
          emptyMessage={mensajeVacio}
        />
      </div>

      {/* Celular: tarjetas */}
      <ul className="cotizaciones-tarjetas">
        {visibles.map((c) => (
          <li key={c.id} className="cotizacion-tarjeta">
            <div className="cotizacion-tarjeta-fila">
              <strong>{c.folio}</strong>
              <strong>{formatoMoneda(c.total)}</strong>
            </div>
            <p>{[c.cliente, c.empresa].filter(Boolean).join(" · ")}</p>
            <Chip category="cotizacion" status={chipDeEstado(c.estado)} />
            <p className="cotizacion-tarjeta-fecha">
              Creada el {formatoFecha(c.fechaCreacion)}
            </p>
            <BotonVer cotizacion={c} />
          </li>
        ))}
        {visibles.length === 0 && (
          <li className="cotizaciones-vacio">{mensajeVacio}</li>
        )}
      </ul>
    </div>
  );
}
