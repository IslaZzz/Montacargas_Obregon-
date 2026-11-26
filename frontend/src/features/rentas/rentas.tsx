import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Chip, { claseDeChip } from "../../components/chips";
import DateRangePicker from "../../components/filters/dateRangePicker";
import FilterRow from "../../components/filters/filterRow";
import type { DateRangeValue } from "../../components/filters/filterTypes";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { formatoDiaCorto } from "../../utils/fechas";
import type { Renta } from "./estadosRentaTypes";
import { listarRentas } from "./rentasApi";
import "../../styles/operacion.css";
// El resumen de la derecha usa las mismas clases que el de Equipos, para que las dos consultas se vean iguales
import "../../styles/equipos/consultar-equipos.css";

interface RentaVista extends Renta {
  clienteNombre: string;
  equiposResumen: string;
  categoriaFiltro: Categoria;
}

type Categoria = "APARTADOS_PROXIMOS" | "POR_VENCER" | "PENDIENTES_RECOLECCION" | "TODAS";

const DIAS_AVISO = 7;

// Chips de "Filtrar por", con el color de su punto
const CATEGORIAS: { valor: Exclude<Categoria, "TODAS">; nombre: string; punto: string }[] = [
  { valor: "APARTADOS_PROXIMOS", nombre: "Apartados próximos", punto: claseDeChip("renta", "programado") },
  { valor: "POR_VENCER", nombre: "Por vencer", punto: claseDeChip("renta", "en-curso") },
  { valor: "PENDIENTES_RECOLECCION", nombre: "Pendientes de recolección", punto: claseDeChip("traslado", "recoleccion") },
];

function fechaLocal(fecha: Date): string {
  const year = fecha.getFullYear();
  const month = String(fecha.getMonth() + 1).padStart(2, "0");
  const day = String(fecha.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function diasEntre(fecha: string): number {
  const hoy = new Date(`${fechaLocal(new Date())}T00:00:00`);
  const objetivo = new Date(`${fecha}T00:00:00`);
  return Math.round((objetivo.getTime() - hoy.getTime()) / 86400000);
}

function formatearFecha(fecha: string): string {
  if (!fecha) return "Sin fecha";
  return new Date(`${fecha}T00:00:00`).toLocaleDateString("es-MX", {
    day: "numeric",
    month: "long",
  });
}

function obtenerNombreCliente(renta: Renta): string {
  const cliente = renta.cliente;
  return (
    cliente.empresa ||
    [cliente.nombre, cliente.apellidoPaterno, cliente.apellidoMaterno].filter(Boolean).join(" ")
  );
}

const pendienteDeRecoleccion = (renta: Renta) =>
  renta.equipos.some((equipo) => ["EN_OPERACION", "EN_REVISION_ENTRADA"].includes(equipo.fase));

const porVencer = (renta: Renta) => {
  const dias = diasEntre(renta.fechaFin);
  return renta.estado === "EN_CURSO" && dias >= 0 && dias <= DIAS_AVISO;
};

function obtenerCategoria(renta: Renta): Categoria {
  const diasInicio = diasEntre(renta.fechaInicio);
  if (pendienteDeRecoleccion(renta)) return "PENDIENTES_RECOLECCION";
  if (porVencer(renta)) return "POR_VENCER";
  if (renta.estado === "PROGRAMADA" && diasInicio >= 0 && diasInicio <= DIAS_AVISO) return "APARTADOS_PROXIMOS";
  return "TODAS";
}

function obtenerEstadoChip(estado: Renta["estado"]) {
  const estados = {
    PROGRAMADA: "programado",
    EN_CURSO: "en-curso",
    CERRADA: "finalizado",
    CANCELADA: "cancelado",
  } as const;
  return estados[estado];
}

// Sin acentos y en minúsculas, para que "agricola" encuentre "Agrícola"
const normalizar = (texto: string) =>
  texto
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

// Conteos del panel de la derecha
const RESUMEN: { nombre: string; punto: string; contar: (rentas: Renta[]) => number }[] = [
  {
    nombre: "Rentas programadas",
    punto: claseDeChip("renta", "programado"),
    contar: (rentas) => rentas.filter((r) => r.estado === "PROGRAMADA").length,
  },
  {
    nombre: "Recolecciones pendientes",
    punto: claseDeChip("traslado", "recoleccion"),
    contar: (rentas) => rentas.filter(pendienteDeRecoleccion).length,
  },
  {
    nombre: "Por vencer",
    punto: claseDeChip("renta", "en-curso"),
    contar: (rentas) => rentas.filter(porVencer).length,
  },
];

/** Consultar rentas: lista con buscador, fechas y filtros, y el resumen a la derecha. */
export default function Rentas() {
  useTitulo("Rentas");
  const navigate = useNavigate();

  const [rentas, setRentas] = useState<Renta[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  // Filtros: texto (cliente o folio), periodo y chip de categoría ("all" = todas)
  const [texto, setTexto] = useState("");
  const [fechas, setFechas] = useState<DateRangeValue>({ from: "", to: "" });
  const [categoria, setCategoria] = useState("all");

  useEffect(() => {
    listarRentas()
      .then(setRentas)
      .catch(() => setError("No fue posible cargar las rentas."))
      .finally(() => setCargando(false));
  }, []);

  const vistas = useMemo<RentaVista[]>(
    () =>
      rentas.map((renta) => ({
        ...renta,
        clienteNombre: obtenerNombreCliente(renta),
        equiposResumen: renta.equipos.map((equipo) => equipo.numeroUnidad).join(", "),
        categoriaFiltro: obtenerCategoria(renta),
      })),
    [rentas],
  );

  // Rentas que están vigentes en algún día del periodo elegido
  const enPeriodo = vistas.filter(
    (r) => (!fechas.from || r.fechaFin >= fechas.from) && (!fechas.to || r.fechaInicio <= fechas.to),
  );

  const buscado = normalizar(texto.trim());
  const encontradas = enPeriodo.filter((r) => !buscado || normalizar(`${r.clienteNombre} ${r.folio}`).includes(buscado));
  const visibles = categoria === "all" ? encontradas : encontradas.filter((r) => r.categoriaFiltro === categoria);

  const filtrosCategoria = CATEGORIAS.map((c) => ({
    label: c.nombre,
    value: c.valor,
    count: encontradas.filter((r) => r.categoriaFiltro === c.valor).length,
    dotClassName: c.punto,
  }));

  const textoFechas =
    fechas.from && fechas.to
      ? `${formatoDiaCorto(fechas.from)} – ${formatoDiaCorto(fechas.to)}`
      : fechas.from
        ? `Desde ${formatoDiaCorto(fechas.from)}`
        : fechas.to
          ? `Hasta ${formatoDiaCorto(fechas.to)}`
          : "Todas";

  const columnas: TableColumn<RentaVista>[] = [
    {
      header: "Renta",
      accessor: (r) => (
        <>
          <span className="celda-principal">{r.clienteNombre}</span>
          <span className="celda-secundaria">{r.folio}</span>
        </>
      ),
    },
    {
      header: "Equipos",
      accessor: (r) => (
        <>
          <span className="celda-principal">
            {r.equipos.length} {r.equipos.length === 1 ? "equipo" : "equipos"}
          </span>
          <span className="celda-secundaria">{r.equiposResumen || "Sin equipos"}</span>
        </>
      ),
    },
    {
      header: "Periodo",
      accessor: (r) => (
        <>
          <span className="celda-principal">Inicia {formatearFecha(r.fechaInicio)}</span>
          <span className="celda-secundaria">Termina {formatearFecha(r.fechaFin)}</span>
        </>
      ),
    },
    {
      header: "Estado",
      accessor: (r) => <Chip category="renta" status={obtenerEstadoChip(r.estado)} />,
    },
    {
      header: "Acciones",
      className: "celda-accion",
      accessor: (r) => (
        <span className="acciones">
          <Button type="button" variant="primary" className="btn-chico" onClick={() => navigate(`/rentas/${r.id}`)}>
            Ver renta
            <span className="material-symbols-rounded" aria-hidden="true">
              arrow_forward
            </span>
          </Button>
        </span>
      ),
    },
  ];

  return (
    <div className="equipos-estados">
      <div className="pagina operacion">
        {/* Título */}
        <div className="operacion-titulo">
          <h1>Rentas</h1>
          <p>Consulta las rentas y encuentra las que están por empezar, por vencer o pendientes de recoger.</p>
        </div>

        {error && <Aviso tipo="error" titulo={error} />}

        {/* Buscador y fechas */}
        <div className="operacion-filtros-izquierda">
          <label className="operacion-filtro">
            <span className="operacion-filtro-etiqueta">Buscar:</span>
            <input
              type="search"
              className="campo-input equipos-buscador"
              placeholder="Cliente o folio"
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
            />
          </label>
          <div className="operacion-filtro">
            <span className="operacion-filtro-etiqueta">Fecha:</span>
            <DateRangePicker
              value={fechas}
              onChange={(campo, valor) => setFechas((prev) => ({ ...prev, [campo]: valor }))}
            />
          </div>
        </div>

        {/* Categoría */}
        <div className="operacion-filtro">
          <span className="operacion-filtro-etiqueta">Filtrar por:</span>
          <FilterRow
            options={filtrosCategoria}
            selectedValue={categoria}
            allLabel="Todas"
            allCount={encontradas.length}
            onChange={setCategoria}
          />
        </div>

        <DataTable
          columns={columnas}
          data={visibles}
          getRowKey={(r) => r.id}
          emptyMessage={cargando ? "Cargando rentas…" : "No hay rentas con estos filtros."}
        />
      </div>

      {/* Resumen: mismo panel que en Equipos */}
      <aside className="equipos-resumen" aria-labelledby="titulo-resumen-rentas">
        <div className="equipos-resumen-encabezado">
          <h2 id="titulo-resumen-rentas">Resumen</h2>
        </div>
        <div className="equipos-resumen-fechas">
          <span className="material-symbols-rounded" aria-hidden="true">
            schedule
          </span>
          Fechas: {textoFechas}
        </div>
        {RESUMEN.map((item) => (
          <div key={item.nombre} className="equipos-conteo">
            <span className="equipos-conteo-numero">{cargando ? "–" : item.contar(enPeriodo)}</span>
            <span className="equipos-conteo-texto">
              <span className={`equipos-punto ${item.punto}`} aria-hidden="true" />
              {item.nombre}
            </span>
          </div>
        ))}
      </aside>
    </div>
  );
}