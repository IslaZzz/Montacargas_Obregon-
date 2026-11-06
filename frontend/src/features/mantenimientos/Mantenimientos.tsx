import { useEffect, useState } from "react";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Chip from "../../components/chips";
import DateRangePicker from "../../components/filters/dateRangePicker";
import FilterRow from "../../components/filters/filterRow";
import type { DateRangeValue } from "../../components/filters/filterTypes";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import AsignarMecanicoModal from "./AsignarMecanicoModal";
import { listarMantenimientos } from "./mantenimientosApi";
import type { Mantenimiento, MecanicoCarga } from "./mantenimientosTypes";
import {
  ESTADOS,
  chipDeEstado,
  formatoDiaCorto,
  formatoHora,
  formatoRelativo,
  iniciales,
  ordenar,
} from "./mantenimientosUtils";
import "../../styles/mantenimientos/mantenimientos.css";

export default function Mantenimientos() {
  useTitulo("Mantenimientos");

  const [mantenimientos, setMantenimientos] = useState<Mantenimiento[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  // Filtros: "all" muestra todos los estados
  const [estado, setEstado] = useState("all");
  const [salida, setSalida] = useState<DateRangeValue>({ from: "", to: "" });

  // Ids marcados con la casilla, para asignar varios a la vez
  const [seleccion, setSeleccion] = useState<Set<number>>(new Set());

  // Lo que se está asignando en el modal; vacío = cerrado
  const [enModal, setEnModal] = useState<Mantenimiento[]>([]);

  // Confirmación después de asignar y filas que se resaltan
  const [exito, setExito] = useState<{ titulo: string; detalle: string } | null>(null);
  const [resaltados, setResaltados] = useState<Set<number>>(new Set());

  useEffect(() => {
    // Se ejecuta una vez al abrir la pantalla
    listarMantenimientos()
      .then((lista) => setMantenimientos(ordenar(lista)))
      .catch(() => setError("No fue posible cargar los mantenimientos."))
      .finally(() => setCargando(false));
  }, []);

  // Al cambiar un filtro se limpia la selección, para no asignar equipos que ya no se ven
  function cambiarEstado(valor: string) {
    setEstado(valor);
    setSeleccion(new Set());
  }
  function cambiarSalida(valor: DateRangeValue) {
    setSalida(valor);
    setSeleccion(new Set());
  }

  // Aplica el filtro de estado y el de fecha de salida
  const visibles = mantenimientos.filter((m) => {
    if (estado !== "all" && m.estado !== estado) return false;
    const dia = m.fechaLimite?.slice(0, 10) ?? "";
    if (salida.from && (!dia || dia < salida.from)) return false;
    if (salida.to && (!dia || dia > salida.to)) return false;
    return true;
  });

  // Un chip por estado, con cuántos mantenimientos tiene
  const filtros = ESTADOS.map((e) => ({
    label: e.etiqueta,
    value: e.valor,
    count: mantenimientos.filter((m) => m.estado === e.valor).length,
  }));

  // Solo los pendientes se pueden marcar para asignar en grupo
  const marcables = visibles.filter((m) => m.estado === "PENDIENTE");
  const todosMarcados =
    marcables.length > 0 && marcables.every((m) => seleccion.has(m.id));
  const seleccionados = visibles.filter((m) => seleccion.has(m.id));

  function marcar(id: number) {
    setSeleccion((anterior) => {
      const nueva = new Set(anterior);
      if (nueva.has(id)) nueva.delete(id);
      else nueva.add(id);
      return nueva;
    });
  }

  function marcarTodos() {
    setSeleccion(todosMarcados ? new Set() : new Set(marcables.map((m) => m.id)));
  }

  function abrirModal(lista: Mantenimiento[]) {
    setExito(null);
    setEnModal(lista);
  }

  // Después de asignar: actualiza la tabla, resalta las filas y muestra la confirmación
  function alAsignar(actualizados: Mantenimiento[], mecanico: MecanicoCarga) {
    const porId = new Map(actualizados.map((m) => [m.id, m]));
    setMantenimientos((anterior) => ordenar(anterior.map((m) => porId.get(m.id) ?? m)));

    const n = actualizados.length;
    setExito({
      titulo:
        n === 1
          ? `${actualizados[0].numeroUnidad} quedó asignado a ${mecanico.nombre}`
          : `${n} equipos quedaron asignados a ${mecanico.nombre}`,
      detalle: "Te los marcamos en la tabla.",
    });
    setResaltados(new Set(porId.keys()));
    setSeleccion(new Set());
    setEnModal([]);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function accion(m: Mantenimiento) {
    if (m.estado === "PENDIENTE") {
      return (
        <Button type="button" variant="primary" className="btn-chico" onClick={() => abrirModal([m])}>
          <span className="material-symbols-rounded" aria-hidden="true">person_add</span>
          Asignar
        </Button>
      );
    }
    if (m.estado === "ASIGNADO" || m.estado === "EN_PROCESO") {
      return (
        <Button type="button" variant="tertiary" className="btn-chico" onClick={() => abrirModal([m])}>
          <span className="material-symbols-rounded" aria-hidden="true">swap_horiz</span>
          Cambiar
        </Button>
      );
    }
    // Terminados y cancelados no tienen acción en esta historia
    return null;
  }

  const columnas: TableColumn<Mantenimiento>[] = [
    {
      header: (
        <input
          type="checkbox"
          aria-label="Seleccionar todos los pendientes"
          checked={todosMarcados}
          disabled={marcables.length === 0}
          onChange={marcarTodos}
        />
      ),
      accessor: (m) => (
        <input
          type="checkbox"
          aria-label={`Seleccionar ${m.numeroUnidad}`}
          checked={seleccion.has(m.id)}
          disabled={m.estado !== "PENDIENTE"}
          onChange={() => marcar(m.id)}
        />
      ),
      className: "celda-casilla",
    },
    {
      header: "Equipo",
      accessor: (m) => (
        <>
          <span className="celda-principal">{m.numeroUnidad}</span>
          <span className="celda-secundaria">{m.descripcionEquipo}</span>
        </>
      ),
    },
    {
      header: "Falla detectada",
      accessor: (m) => (
        <>
          <span className="celda-falla">
            <span
              className={"material-symbols-rounded " + (m.estado === "COMPLETADO" ? "icono-ok" : "icono-falla")}
              aria-hidden="true"
            >
              {m.estado === "COMPLETADO" ? "check_circle" : "warning"}
            </span>
            {m.descripcionFalla}
          </span>
          <span className="celda-secundaria">
            {[m.reportadoPor && `Reportó ${m.reportadoPor}`, formatoRelativo(m.fechaReporte)]
              .filter(Boolean)
              .join(" · ")}
          </span>
        </>
      ),
    },
    {
      header: "Salida programada",
      accessor: (m) =>
        m.fechaLimite ? (
          <>
            <span className="celda-principal">{formatoDiaCorto(m.fechaLimite)}</span>
            <span className="celda-secundaria">
              {formatoHora(m.fechaLimite)}
              {m.folioRenta && ` · Renta ${m.folioRenta}`}
            </span>
          </>
        ) : (
          "—"
        ),
    },
    {
      header: "Mecánico",
      accessor: (m) =>
        m.mecanicoNombre ? (
          <div className="celda-mecanico">
            <span className="avatar" aria-hidden="true">{iniciales(m.mecanicoNombre)}</span>
            <div>
              <span className="celda-mecanico-nombre">{m.mecanicoNombre}</span>
              <Chip category="mantenimiento" status={chipDeEstado(m.estado)} />
            </div>
          </div>
        ) : (
          <Chip category="mantenimiento" status="sin-asignar" />
        ),
    },
    { header: "Acción", accessor: accion, className: "celda-accion" },
  ];

  return (
    <div className="pagina mantenimientos">
      {/* Título */}
      <div className="mantenimientos-titulo">
        <h1>Mantenimientos</h1>
        <p>Asigna a un mecánico los equipos con falla.</p>
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

      {/* Filtros */}
      <div className="mantenimientos-filtros">
        <div className="mantenimientos-filtro">
          <span className="mantenimientos-filtro-etiqueta">Mostrar:</span>
          <FilterRow
            options={filtros}
            selectedValue={estado}
            allLabel="Todos"
            allCount={mantenimientos.length}
            onChange={cambiarEstado}
          />
        </div>
        <div className="mantenimientos-filtro">
          <span className="mantenimientos-filtro-etiqueta">Salida:</span>
          <DateRangePicker
            value={salida}
            onChange={(campo, valor) => cambiarSalida({ ...salida, [campo]: valor })}
          />
        </div>
      </div>

      {/* Barra que aparece al marcar equipos */}
      {seleccionados.length > 0 && (
        <div className="mantenimientos-seleccion">
          <span>
            {seleccionados.length}{" "}
            {seleccionados.length === 1 ? "equipo seleccionado" : "equipos seleccionados"}
          </span>
          <Button type="button" variant="primary" onClick={() => abrirModal(seleccionados)}>
            <span className="material-symbols-rounded" aria-hidden="true">person_add</span>
            Asignar a mecánico
          </Button>
        </div>
      )}

      {/* Tabla */}
      <DataTable
        columns={columnas}
        data={visibles}
        getRowKey={(m) => m.id}
        getRowClassName={(m) => (resaltados.has(m.id) ? "fila-resaltada" : undefined)}
        emptyMessage={cargando ? "Cargando mantenimientos…" : "No hay mantenimientos con estos filtros."}
      />

      <AsignarMecanicoModal
        mantenimientos={enModal}
        onCerrar={() => setEnModal([])}
        onAsignado={alAsignar}
      />
    </div>
  );
}