import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import Aviso from "../../components/Aviso";
import Avatar from "../../components/Avatar";
import { Button } from "../../components/buttons";
import Chip from "../../components/chips";
import DateRangePicker from "../../components/filters/dateRangePicker";
import FilterRow from "../../components/filters/filterRow";
import type { DateRangeValue } from "../../components/filters/filterTypes";
import Modal from "../../components/Modal";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { ErrorApi } from "../../services/apiClient";
import { formatoDiaCorto, formatoHora } from "../../utils/fechas";
import PanelAsignacion from "./PanelAsignacion";
import { listarTraslados, marcarRealizado } from "./trasladosApi";
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

  // Traslado que se está marcando como realizado; null = cerrado
  const [enRealizar, setEnRealizar] = useState<Traslado | null>(null);
  const [realizando, setRealizando] = useState(false);
  const [errorRealizar, setErrorRealizar] = useState("");

  // Confirmación después de asignar o marcar realizado, y fila que se resalta
  const [exito, setExito] = useState<{ titulo: string; detalle: ReactNode } | null>(null);
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

  // Reemplaza el traslado en la lista y lo resalta
  function actualizar(actualizado: Traslado, titulo: string, detalle: ReactNode) {
    setTraslados((prev) => prev.map((t) => (t.id === actualizado.id ? actualizado : t)));
    setExito({ titulo, detalle });
    setResaltados(new Set([actualizado.id]));
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function handleAsignacionExitosa(actualizado: Traslado) {
    actualizar(
      actualizado,
      `${actualizado.folio} quedó asignado a ${actualizado.responsableNombre}`,
      "Te lo marcamos en la tabla.",
    );
  }

  function abrirRealizar(t: Traslado) {
    setExito(null);
    setErrorRealizar("");
    setEnRealizar(t);
  }

  function cerrarRealizar() {
    if (!realizando) setEnRealizar(null);
  }

  async function confirmarRealizado() {
    if (!enRealizar) return;
    setRealizando(true);
    setErrorRealizar("");
    try {
      const actualizado = await marcarRealizado(enRealizar.id);
      actualizar(
        actualizado,
        `${actualizado.folio} quedó realizado`,
        actualizado.tipo === "ENTREGA" ? (
          `Los equipos de la renta ${actualizado.rentaFolio} ya están con el cliente.`
        ) : (
          <>
            Ya puedes asignar las <Link to="/revisiones">revisiones de entrada</Link> de la renta{" "}
            {actualizado.rentaFolio}.
          </>
        ),
      );
      setEnRealizar(null);
    } catch (err) {
      setErrorRealizar(err instanceof ErrorApi ? err.message : "No fue posible guardar. Revisa tu conexión.");
    } finally {
      setRealizando(false);
    }
  }

  function accion(row: Traslado) {
    if (row.estado === "COMPLETADO") {
      return <Chip category="traslado" status="ok" />;
    }
    if (row.responsableId === null) {
      return (
        <span className="acciones">
          <Button type="button" variant="primary" className="btn-chico" onClick={() => abrirPanel(row)}>
            <span className="material-symbols-rounded" aria-hidden="true">person_add</span>
            Asignar
          </Button>
        </span>
      );
    }
    return (
      <span className="acciones">
        <Button type="button" variant="primary" className="btn-chico" onClick={() => abrirRealizar(row)}>
          <span className="material-symbols-rounded" aria-hidden="true">task_alt</span>
          Marcar realizado
        </Button>
        {/* Si el operador ya salió, ya no se puede cambiar */}
        {!row.yaInicio && (
          <Button type="button" variant="tertiary" className="btn-chico" onClick={() => abrirPanel(row)}>
            <span className="material-symbols-rounded" aria-hidden="true">swap_horiz</span>
            Cambiar
          </Button>
        )}
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
        <p>Elige quién lleva cada entrega y recolección, y márcala cuando el operador confirme que la hizo.</p>
      </div>

      {/* Avisos */}
      {exito && (
        <Aviso tipo="exito" titulo={exito.titulo} onCerrar={() => setExito(null)}>
          {exito.detalle}
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

      {/* Confirmar que el traslado ya se hizo */}
      <Modal
        abierto={enRealizar !== null}
        titulo={
          enRealizar
            ? `¿Ya se ${enRealizar.tipo === "ENTREGA" ? "entregó" : "recogió"} ${enRealizar.folio}?`
            : ""
        }
        subtitulo="Márcalo cuando el operador te confirme que lo hizo."
        onCerrar={cerrarRealizar}
        acciones={
          <>
            <Button type="button" variant="tertiary" onClick={cerrarRealizar} disabled={realizando}>
              Cancelar
            </Button>
            <Button type="button" variant="primary" onClick={confirmarRealizado} disabled={realizando}>
              {realizando ? "Guardando…" : "Marcar realizado"}
            </Button>
          </>
        }
      >
        {enRealizar && (
          <div className="traslados-realizado">
            <p>
              <strong>Cliente:</strong> {enRealizar.clienteNombre}
            </p>
            <p>
              <strong>Operador:</strong> {enRealizar.responsableNombre}
            </p>
            <p>
              {enRealizar.tipo === "ENTREGA"
                ? `Los equipos quedarán con el cliente y la renta ${enRealizar.rentaFolio} pasará a en curso.`
                : `Se crearán las revisiones de entrada de los equipos de la renta ${enRealizar.rentaFolio}.`}
            </p>
            {errorRealizar && (
              <p className="campo-error" role="alert">
                {errorRealizar}
              </p>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}