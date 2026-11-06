import { useEffect, useState } from "react";
import Aviso from "../../components/Aviso";
import Avatar from "../../components/Avatar";
import { Button } from "../../components/buttons";
import Chip from "../../components/chips";
import DateRangePicker from "../../components/filters/dateRangePicker";
import FilterRow from "../../components/filters/filterRow";
import type { DateRangeValue } from "../../components/filters/filterTypes";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { formatoDiaCorto, formatoHora } from "../../utils/fechas";
import PanelAsignacion from "./PanelAsignacion";
import { listarTraslados } from "./trasladosApi";
import type { FiltroTraslados, Traslado } from "./trasladosTypes";
import { CHIPS_FILTRO, contarPorFiltro } from "./trasladosUtils";
import "../../styles/operacion.css";
import "../../styles/traslados/traslados.css";

export default function DashboardTraslados() {
  useTitulo("Traslados");

  const [traslados, setTraslados] = useState<Traslado[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  // Filtro de la fila de chips; "TODOS" es el chip de todos
  const [filtroTipo, setFiltroTipo] = useState<FiltroTraslados>("TODOS");

  // Rango de fechas; vacío muestra todos
  const [rangoFechas, setRangoFechas] = useState<DateRangeValue>({ from: "", to: "" });

  // Traslado que se está asignando en el panel; null = cerrado
  const [trasladoEnPanel, setTrasladoEnPanel] = useState<Traslado | null>(null);

  // Confirmación después de asignar y fila que se resalta
  const [exito, setExito] = useState<string | null>(null);
  const [resaltados, setResaltados] = useState<Set<number>>(new Set());

  // Carga inicial y recarga al cambiar el rango de fechas
  useEffect(() => {
    const controller = new AbortController();

    listarTraslados(undefined, rangoFechas.from || undefined, rangoFechas.to || undefined)
      .then((datos) => {
        if (!controller.signal.aborted) {
          setTraslados(datos);
          setCargando(false);
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setError("No fue posible cargar los traslados.");
          setCargando(false);
        }
      });

    return () => {
      controller.abort();
    };
  }, [rangoFechas.from, rangoFechas.to]);

  // Filtrado en memoria por el chip seleccionado
  const trasladosVisibles = traslados.filter((t) => {
    if (filtroTipo === "SIN_ASIGNAR" && t.responsableId !== null) return false;
    if (filtroTipo === "ENTREGAS" && t.tipo !== "ENTREGA") return false;
    if (filtroTipo === "RECOLECCIONES" && t.tipo !== "RECOLECCION") return false;
    return true;
  });

  // Un chip por filtro, con cuántos traslados tiene y el color que lo representa
  const filtros = CHIPS_FILTRO.map((chip) => ({
    label: chip.label,
    value: chip.id,
    count: contarPorFiltro(traslados, chip.id),
    dotClassName: chip.dotClassName,
  }));

  function abrirPanel(t: Traslado) {
    setExito(null);
    setTrasladoEnPanel(t);
  }

  function handleAsignacionExitosa(actualizado: Traslado) {
    setTraslados((prev) => prev.map((t) => (t.id === actualizado.id ? actualizado : t)));
    setExito(`${actualizado.folio} quedó asignado a ${actualizado.responsableNombre}`);
    setResaltados(new Set([actualizado.id]));
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function accion(row: Traslado) {
    // Si el operador ya salió, ya no se puede cambiar
    if (row.yaInicio) {
      return (
        <span className="acciones">
          <Button
            type="button"
            variant="tertiary"
            className="btn-chico"
            disabled
            title="El operador ya inició este traslado y no se puede modificar"
          >
            <span className="material-symbols-rounded" aria-hidden="true">lock</span>
            Ya inició
          </Button>
        </span>
      );
    }
    const asignado = row.responsableId !== null;
    return (
      <span className="acciones">
        <Button
          type="button"
          variant={asignado ? "tertiary" : "primary"}
          className="btn-chico"
          onClick={() => abrirPanel(row)}
        >
          <span className="material-symbols-rounded" aria-hidden="true">
            {asignado ? "swap_horiz" : "person_add"}
          </span>
          {asignado ? "Cambiar" : "Asignar"}
        </Button>
      </span>
    );
  }

  const columnas: TableColumn<Traslado>[] = [
    {
      header: "Traslado",
      accessor: (row) => (
        <>
          <span className="celda-principal">{row.folio}</span>
          <span className="celda-secundaria">Renta {row.rentaFolio}</span>
        </>
      ),
    },
    {
      header: "Tipo",
      accessor: (row) => (
        <Chip category="traslado" status={row.tipo === "ENTREGA" ? "entrega" : "recoleccion"} />
      ),
    },
    {
      header: "Fecha y horario",
      accessor: (row) => (
        <>
          <span className="celda-principal">{formatoDiaCorto(row.fechaHoraInicio)}</span>
          <span className="celda-secundaria">
            {formatoHora(row.fechaHoraInicio)} – {formatoHora(row.fechaHoraFin)}
          </span>
        </>
      ),
    },
    {
      header: "Cliente y domicilio",
      accessor: (row) => (
        <>
          <span className="celda-principal">{row.clienteNombre}</span>
          <span className="celda-secundaria traslados-domicilio">{row.direccion}</span>
        </>
      ),
    },
    {
      header: "Responsable",
      accessor: (row) =>
        row.responsableNombre ? (
          <div className="celda-persona">
            <Avatar nombre={row.responsableNombre} />
            <span className="celda-persona-nombre">{row.responsableNombre}</span>
          </div>
        ) : (
          <Chip category="traslado" status="sin-asignar" />
        ),
    },
    { header: "Acción", accessor: accion, className: "celda-accion" },
  ];

  return (
    <div className="pagina operacion traslados">
      {/* Título */}
      <div className="operacion-titulo">
        <h1>Traslados</h1>
        <p>Elige quién lleva cada entrega y recolección programada.</p>
      </div>

      {/* Avisos */}
      {exito && (
        <Aviso tipo="exito" titulo={exito} onCerrar={() => setExito(null)}>
          Te lo marcamos en la tabla.
        </Aviso>
      )}
      {error && (
        <Aviso tipo="error" titulo={error}>
          Revisa tu conexión y vuelve a abrir la pantalla.
        </Aviso>
      )}

      {/* Filtros: tipo a la izquierda, fechas a la derecha */}
      <div className="operacion-filtros">
        <div className="operacion-filtro">
          <span className="operacion-filtro-etiqueta">Mostrar:</span>
          <FilterRow
            options={filtros}
            selectedValue={filtroTipo === "TODOS" ? "all" : filtroTipo}
            allLabel="Todos"
            allCount={traslados.length}
            onChange={(valor) => setFiltroTipo(valor === "all" ? "TODOS" : (valor as FiltroTraslados))}
          />
        </div>
        <div className="operacion-filtro">
          <span className="operacion-filtro-etiqueta">Fecha:</span>
          <DateRangePicker
            value={rangoFechas}
            onChange={(campo, valor) => setRangoFechas((prev) => ({ ...prev, [campo]: valor }))}
          />
        </div>
      </div>

      {/* Tabla */}
      <DataTable
        columns={columnas}
        data={trasladosVisibles}
        getRowKey={(row) => row.id}
        getRowClassName={(row) => (resaltados.has(row.id) ? "fila-resaltada" : undefined)}
        emptyMessage={cargando ? "Cargando traslados…" : "No hay traslados con estos filtros."}
      />

      {/* Panel de asignación o cambio de responsable */}
      <PanelAsignacion
        abierto={trasladoEnPanel !== null}
        traslado={trasladoEnPanel}
        onCerrar={() => setTrasladoEnPanel(null)}
        onAsignacionExitosa={handleAsignacionExitosa}
      />
    </div>
  );
}