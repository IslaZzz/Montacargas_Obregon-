import { useEffect, useState } from "react";
import Avatar from "../../components/Avatar";
import DateRangePicker from "../../components/filters/dateRangePicker";
import type { DateRangeValue } from "../../components/filters/filterTypes";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import PanelAsignacion from "./PanelAsignacion";
import { listarTraslados } from "./trasladosApi";
import type { FiltroTraslados, Traslado } from "./trasladosTypes";
import {
  CHIPS_FILTRO,
  contarPorFiltro,
  formatoDiaSemanaFecha,
  formatoRangoHorario,
  parsearFechaLocal,
} from "./trasladosUtils";
import "../../styles/traslados/traslados.css";

export default function DashboardTraslados() {
  useTitulo("Asignar Traslados");

  const [traslados, setTraslados] = useState<Traslado[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  // Filtro activo de la botonera de chips
  const [filtroTipo, setFiltroTipo] = useState<FiltroTraslados>("TODOS");

  // Rango de fechas seleccionado (inicializado con 2026-10-07 a 2026-10-19 según diseño o configurable)
  const [rangoFechas, setRangoFechas] = useState<DateRangeValue>({
    from: "",
    to: "",
  });
  const [mostrarPopoverFecha, setMostrarPopoverFecha] = useState(false);

  // Traslado seleccionado para el panel lateral
  const [trasladoEnPanel, setTrasladoEnPanel] = useState<Traslado | null>(null);

  // Carga inicial y recarga al cambiar filtros de fecha
  useEffect(() => {
    const controller = new AbortController();

    listarTraslados(undefined, rangoFechas.from || undefined, rangoFechas.to || undefined)
      .then((datos) => {
        if (!controller.signal.aborted) {
          setTraslados(datos);
          setCargando(false);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error ? err.message : "No fue posible cargar los traslados."
          );
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

  function handleAsignacionExitosa(actualizado: Traslado) {
    setTraslados((prev) =>
      prev.map((t) => (t.id === actualizado.id ? actualizado : t))
    );
  }

  // Texto amigable para el botón de fecha en el encabezado
  function textoBotonFecha(): string {
    if (!rangoFechas.from && !rangoFechas.to) {
      return "Filtrar por fecha";
    }
    if (rangoFechas.from && rangoFechas.to) {
      const d1 = parsearFechaLocal(rangoFechas.from);
      const d2 = parsearFechaLocal(rangoFechas.to);
      const pad = (n: number) => String(n).padStart(2, "0");
      const meses = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
      if (d1.getMonth() === d2.getMonth()) {
        return `Fecha: ${pad(d1.getDate())} - ${pad(d2.getDate())} ${meses[d1.getMonth()]}`;
      }
      return `Fecha: ${pad(d1.getDate())} ${meses[d1.getMonth()]} - ${pad(d2.getDate())} ${meses[d2.getMonth()]}`;
    }
    if (rangoFechas.from) {
      return `Desde: ${rangoFechas.from}`;
    }
    return `Hasta: ${rangoFechas.to}`;
  }

  // Definición de columnas para DataTable
  const columnas: TableColumn<Traslado>[] = [
    {
      header: "Traslado",
      accessor: (row) => (
        <div className="col-traslado-info">
          <span className="col-traslado-folio">{row.folio}</span>
          <span className="col-traslado-renta">Renta {row.rentaFolio}</span>
        </div>
      ),
    },
    {
      header: "Tipo",
      accessor: (row) => (
        <span
          className={`badge-tipo ${
            row.tipo === "ENTREGA" ? "badge-tipo-entrega" : "badge-tipo-recoleccion"
          }`}
        >
          <span className="material-symbols-rounded">
            {row.tipo === "ENTREGA" ? "local_shipping" : "swap_horiz"}
          </span>
          {row.tipo === "ENTREGA" ? "Entrega" : "Recolección"}
        </span>
      ),
    },
    {
      header: "Fecha y horario",
      accessor: (row) => (
        <div className="col-fecha-bloque">
          <span className="col-fecha-dia">
            {formatoDiaSemanaFecha(row.fechaHoraInicio)}
          </span>
          <span className="col-fecha-horas">
            {formatoRangoHorario(row.fechaHoraInicio, row.fechaHoraFin)}
          </span>
        </div>
      ),
    },
    {
      header: "Cliente y domicilio",
      accessor: (row) => (
        <div className="col-cliente-bloque">
          <span className="col-cliente-nombre">{row.clienteNombre}</span>
          <span className="col-cliente-domicilio">{row.direccion}</span>
        </div>
      ),
    },
    {
      header: "Responsable",
      accessor: (row) => {
        if (row.responsableId && row.responsableNombre) {
          return (
            <div className="col-responsable-bloque">
              <Avatar nombre={row.responsableNombre} />
              <span className="responsable-nombre-txt">{row.responsableNombre}</span>
            </div>
          );
        }
        return (
          <span className="badge-sin-asignar">
            <span className="material-symbols-rounded">person_cancel</span>
            Sin asignar
          </span>
        );
      },
    },
    {
      header: "",
      className: "col-acciones-celda",
      accessor: (row) => {
        // Reglas de interfaz para los botones de acción:
        // 1. Si ya inició en campo -> Botón "Ya inició" (Deshabilitado con candado)
        if (row.yaInicio) {
          return (
            <button
              type="button"
              className="btn-accion-traslado btn-accion-ya-inicio"
              disabled
              title="El operador ya inició este traslado en campo y no se puede modificar"
            >
              <span className="material-symbols-rounded">lock</span>
              Ya inició
            </button>
          );
        }

        // 2. Si ya tiene responsable -> Botón "Cambiar" (Abre panel para editar)
        if (row.responsableId) {
          return (
            <button
              type="button"
              className="btn-accion-traslado btn-accion-cambiar"
              onClick={() => setTrasladoEnPanel(row)}
            >
              <span className="material-symbols-rounded">swap_horiz</span>
              Cambiar
            </button>
          );
        }

        // 3. Si no tiene responsable -> Botón "Asignar" (Abre panel para asignar)
        return (
          <button
            type="button"
            className="btn-accion-traslado btn-accion-asignar"
            onClick={() => setTrasladoEnPanel(row)}
          >
            <span className="material-symbols-rounded">person_add</span>
            Asignar
          </button>
        );
      },
    },
  ];

  return (
    <div className="pagina traslados">
      {/* Encabezado con Título y Filtro de Fechas */}
      <header className="traslados-header">
        <div className="traslados-titulo-bloque">
          <h1>Asignar traslados</h1>
          <p>Elige quién lleva cada entrega y recolección programada.</p>
        </div>

        {/* Botón y Popover del Rango de Fechas */}
        <div className="traslados-fecha-picker-rel">
          <button
            type="button"
            className="btn-filtro-fecha"
            onClick={() => setMostrarPopoverFecha((prev) => !prev)}
            aria-expanded={mostrarPopoverFecha}
          >
            <span className="material-symbols-rounded">calendar_month</span>
            <span>{textoBotonFecha()}</span>
            <span className="material-symbols-rounded">
              {mostrarPopoverFecha ? "expand_less" : "expand_more"}
            </span>
          </button>

          {mostrarPopoverFecha && (
            <div className="popover-filtro-fecha">
              <DateRangePicker
                value={rangoFechas}
                onChange={(campo, valor) =>
                  setRangoFechas((prev) => ({ ...prev, [campo]: valor }))
                }
              />
              <div className="popover-fecha-acciones">
                <button
                  type="button"
                  className="btn btn-tertiary"
                  style={{ padding: "6px 12px", fontSize: "12px" }}
                  onClick={() => {
                    setRangoFechas({ from: "", to: "" });
                    setMostrarPopoverFecha(false);
                  }}
                >
                  Limpiar
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  style={{ padding: "6px 14px", fontSize: "12px" }}
                  onClick={() => setMostrarPopoverFecha(false)}
                >
                  Listo
                </button>
              </div>
            </div>
          )}
        </div>
      </header>

      {/* Fila de Filtros en Chips */}
      <div className="traslados-filtros-row">
        <span className="traslados-filtros-label">Mostrar:</span>
        {CHIPS_FILTRO.map((chip) => {
          const count = contarPorFiltro(traslados, chip.id);
          const activo = filtroTipo === chip.id;

          return (
            <button
              key={chip.id}
              type="button"
              className={`chip-filtro-btn ${activo ? "activo" : ""}`}
              onClick={() => setFiltroTipo(chip.id)}
            >
              {chip.dotColor && (
                <span
                  className="chip-filtro-dot"
                  style={{ backgroundColor: chip.dotColor }}
                />
              )}
              <span>
                {chip.label} ({count})
              </span>
            </button>
          );
        })}
      </div>

      {/* Estado de Error o Contenido */}
      {error && (
        <div className="panel-error-contextual" style={{ marginBottom: "12px" }}>
          <span className="material-symbols-rounded">error</span>
          <span>{error}</span>
        </div>
      )}

      {/* Tabla Principal */}
      {cargando ? (
        <p style={{ color: "#64748B", fontSize: "14px" }}>Cargando traslados...</p>
      ) : (
        <DataTable
          columns={columnas}
          data={trasladosVisibles}
          getRowKey={(row) => row.id}
          emptyMessage="No hay traslados para el filtro y rango de fechas seleccionados."
        />
      )}

      {/* Panel Lateral de Asignación / Cambio de Responsable */}
      <PanelAsignacion
        abierto={trasladoEnPanel !== null}
        traslado={trasladoEnPanel}
        onCerrar={() => setTrasladoEnPanel(null)}
        onAsignacionExitosa={handleAsignacionExitosa}
      />
    </div>
  );
}
