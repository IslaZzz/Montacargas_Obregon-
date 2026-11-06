import { useEffect, useState } from "react";
import AsignarMecanicoModal from "../../components/AsignarMecanicoModal";
import type { ElementoAsignar, MecanicoOpcion } from "../../components/AsignarMecanicoModal";
import Aviso from "../../components/Aviso";
import Avatar from "../../components/Avatar";
import { Button } from "../../components/buttons";
import Chip, { claseDeChip } from "../../components/chips";
import DateRangePicker from "../../components/filters/dateRangePicker";
import FilterRow from "../../components/filters/filterRow";
import type { DateRangeValue } from "../../components/filters/filterTypes";
import Modal from "../../components/Modal";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { ErrorApi } from "../../services/apiClient";
import { listarMecanicos } from "../../services/personalApi";
import { formatoDiaCorto, formatoHora, formatoRelativo } from "../../utils/fechas";
import { asignarMantenimientos, listarMantenimientos, terminarMantenimiento } from "./mantenimientosApi";
import type { Mantenimiento } from "./mantenimientosTypes";
import { ESTADOS, chipDeEstado, ordenar } from "./mantenimientosUtils";
import "../../styles/operacion.css";
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

  // El que se está marcando como terminado; null = cerrado
  const [enTerminar, setEnTerminar] = useState<Mantenimiento | null>(null);
  const [terminando, setTerminando] = useState(false);
  const [errorTerminar, setErrorTerminar] = useState("");

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
    dotClassName: claseDeChip("mantenimiento", e.chip),
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

  // Guarda la asignación. Si el backend responde error, el modal lo muestra y no se cierra
  async function confirmar(mecanico: MecanicoOpcion) {
    const actualizados = await asignarMantenimientos({
      mantenimientoIds: enModal.map((m) => m.id),
      personalId: mecanico.id,
    });

    // Actualiza la tabla, resalta las filas y muestra la confirmación
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

  function abrirTerminar(m: Mantenimiento) {
    setExito(null);
    setErrorTerminar("");
    setEnTerminar(m);
  }

  function cerrarTerminar() {
    if (!terminando) setEnTerminar(null);
  }

  async function confirmarTerminar() {
    if (!enTerminar) return;
    setTerminando(true);
    setErrorTerminar("");
    try {
      const actualizado = await terminarMantenimiento(enTerminar.id);
      setMantenimientos((anterior) =>
        ordenar(anterior.map((m) => (m.id === actualizado.id ? actualizado : m))),
      );
      setExito({
        titulo: `${actualizado.numeroUnidad} quedó reparado`,
        detalle: "El equipo vuelve a estar operativo. Te lo marcamos en la tabla.",
      });
      setResaltados(new Set([actualizado.id]));
      setEnTerminar(null);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
      setErrorTerminar(err instanceof ErrorApi ? err.message : "No fue posible guardar. Revisa tu conexión.");
    } finally {
      setTerminando(false);
    }
  }

  function accion(m: Mantenimiento) {
    if (m.estado === "PENDIENTE") {
      return (
        <span className="acciones">
          <Button type="button" variant="primary" className="btn-chico" onClick={() => abrirModal([m])}>
            <span className="material-symbols-rounded" aria-hidden="true">person_add</span>
            Asignar
          </Button>
        </span>
      );
    }
    if (m.estado === "ASIGNADO" || m.estado === "EN_PROCESO") {
      return (
        <span className="acciones">
          <Button type="button" variant="primary" className="btn-chico" onClick={() => abrirTerminar(m)}>
            <span className="material-symbols-rounded" aria-hidden="true">task_alt</span>
            Terminar
          </Button>
          <Button type="button" variant="tertiary" className="btn-chico" onClick={() => abrirModal([m])}>
            <span className="material-symbols-rounded" aria-hidden="true">swap_horiz</span>
            Cambiar
          </Button>
        </span>
      );
    }
    // Terminados y cancelados no tienen acción
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
          <div className="celda-persona">
            <Avatar nombre={m.mecanicoNombre} />
            <div>
              <span className="celda-persona-nombre">{m.mecanicoNombre}</span>
              <Chip category="mantenimiento" status={chipDeEstado(m.estado)} />
            </div>
          </div>
        ) : (
          <Chip category="mantenimiento" status="sin-asignar" />
        ),
    },
    { header: "Acción", accessor: accion, className: "celda-accion" },
  ];

  // Datos del modal
  const total = enModal.length;
  const actual = total === 1 ? enModal[0].mecanicoId : null;
  const equipos = `${total} ${total === 1 ? "equipo" : "equipos"}`;
  const titulo =
    actual !== null
      ? `Cambiar el mecánico de ${enModal[0].numeroUnidad}.`
      : `Asignar ${equipos} a un mecánico.`;
  const textoBoton = actual !== null ? "Cambiar mecánico" : `Asignar ${equipos}`;
  const elementos: ElementoAsignar[] = enModal.map((m) => ({
    id: m.id,
    titulo: m.numeroUnidad,
    descripcion: m.descripcionFalla,
    alerta: true,
    detalle: m.fechaLimite
      ? `Sale ${formatoDiaCorto(m.fechaLimite).toLowerCase()} · ${formatoHora(m.fechaLimite)}`
      : "Sin fecha de salida",
  }));

  return (
    <div className="pagina operacion mantenimientos">
      {/* Título */}
      <div className="operacion-titulo">
        <h1>Mantenimientos</h1>
        <p>Asigna a un mecánico los equipos con falla y marca los que ya quedaron reparados.</p>
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
      <div className="operacion-filtros">
        <div className="operacion-filtro">
          <span className="operacion-filtro-etiqueta">Mostrar:</span>
          <FilterRow
            options={filtros}
            selectedValue={estado}
            allLabel="Todos"
            allCount={mantenimientos.length}
            onChange={cambiarEstado}
          />
        </div>
        <div className="operacion-filtro">
          <span className="operacion-filtro-etiqueta">Salida:</span>
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
        abierto={total > 0}
        titulo={titulo}
        subtitulo="Elige quién revisará y reparará las fallas reportadas."
        elementos={elementos}
        mecanicoActualId={actual}
        textoBoton={textoBoton}
        cargarMecanicos={listarMecanicos}
        onConfirmar={confirmar}
        onCerrar={() => setEnModal([])}
      />

      {/* Confirmar que el equipo ya quedó reparado */}
      <Modal
        abierto={enTerminar !== null}
        titulo={enTerminar ? `¿${enTerminar.numeroUnidad} ya quedó reparado?` : ""}
        subtitulo="Márcalo cuando el mecánico te confirme que terminó."
        onCerrar={cerrarTerminar}
        acciones={
          <>
            <Button type="button" variant="tertiary" onClick={cerrarTerminar} disabled={terminando}>
              Cancelar
            </Button>
            <Button type="button" variant="primary" onClick={confirmarTerminar} disabled={terminando}>
              {terminando ? "Guardando…" : "Marcar terminado"}
            </Button>
          </>
        }
      >
        {enTerminar && (
          <div className="mantenimientos-terminar">
            <p>
              <strong>Falla:</strong> {enTerminar.descripcionFalla}
            </p>
            <p>
              <strong>Mecánico:</strong> {enTerminar.mecanicoNombre}
            </p>
            <p>
              El equipo volverá a estar operativo.
              {enTerminar.folioRenta &&
                ` Si era el último pendiente de la renta ${enTerminar.folioRenta}, se creará su traslado de entrega.`}
            </p>
            {errorTerminar && (
              <p className="campo-error" role="alert">
                {errorTerminar}
              </p>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}