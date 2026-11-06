import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Aviso from "../../../components/Aviso";
import { Button } from "../../../components/buttons";
import DateRangePicker from "../../../components/filters/dateRangePicker";
import FilterDropdown from "../../../components/filters/filterDropdown";
import FilterRow from "../../../components/filters/filterRow";
import type { DateRangeValue } from "../../../components/filters/filterTypes";
import DataTable from "../../../components/table";
import type { TableColumn } from "../../../components/table";
import { useTitulo } from "../../../hooks/useTitulo";
import { ErrorApi } from "../../../services/apiClient";
import { formatoDiaCorto } from "../../../utils/fechas";
import { listarCategorias } from "../disponibilidad/disponibilidadApi";
import type { Categoria } from "../disponibilidad/disponibilidadApi";
import { consultarEquiposPorEstado } from "./estadosApi";
import type { EquipoEstado, EstadoConsultaEquipo } from "./estadosTypes";
import { CLASE_ESTADO, ChipEstadoEquipo, ESTADOS, NOMBRE_ESTADO } from "./estadosUtils";
import "../../../styles/operacion.css";
import "../../../styles/equipos/consultar-equipos.css";

const PROXIMAMENTE = "Disponible próximamente";

// Botón que todavía no hace nada; el span muestra el mensaje porque un botón deshabilitado no lo muestra
function BotonProximamente({ icono, texto }: { icono: string; texto: string }) {
  return (
    <span title={PROXIMAMENTE}>
      <Button type="button" variant="tertiary" className="btn-chico" disabled>
        {texto}
        <span className="material-symbols-rounded" aria-hidden="true">{icono}</span>
      </Button>
    </span>
  );
}

// Consulta de equipos por estado, con fechas y categoría
export default function ConsultarEquipos() {
  useTitulo("Equipos");
  const navigate = useNavigate();

  const [equipos, setEquipos] = useState<EquipoEstado[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [categorias, setCategorias] = useState<Categoria[]>([]);

  const [fechas, setFechas] = useState<DateRangeValue>({ from: "", to: "" });
  const [categoria, setCategoria] = useState("all");
  const [estado, setEstado] = useState("all");

  // Solo se consulta sin fechas o con las dos
  const conPeriodo = fechas.from !== "" && fechas.to !== "";
  const periodoIncompleto = (fechas.from !== "") !== (fechas.to !== "");

  useEffect(() => {
    listarCategorias()
      .then(setCategorias)
      .catch(() => setCategorias([]));
  }, []);

  useEffect(() => {
    if (periodoIncompleto) return;
    const controller = new AbortController();
    setCargando(true);
    setError("");

    consultarEquiposPorEstado(conPeriodo ? fechas.from : undefined, conPeriodo ? fechas.to : undefined)
      .then((datos) => {
        if (!controller.signal.aborted) setEquipos(datos);
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setError(err instanceof ErrorApi ? err.message : "No fue posible cargar los equipos.");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setCargando(false);
      });

    return () => controller.abort();
  }, [fechas.from, fechas.to, conPeriodo, periodoIncompleto]);

  // La categoría acota todo; el estado solo la tabla, para que los conteos sigan completos
  const deLaCategoria = categoria === "all" ? equipos : equipos.filter((e) => e.categoria === categoria);
  const visibles = estado === "all" ? deLaCategoria : deLaCategoria.filter((e) => e.estado === estado);
  const contar = (valor: EstadoConsultaEquipo) => deLaCategoria.filter((e) => e.estado === valor).length;

  const filtrosEstado = ESTADOS.map((valor) => ({
    label: NOMBRE_ESTADO[valor],
    value: valor,
    count: contar(valor),
    dotClassName: CLASE_ESTADO[valor],
  }));

  const textoFechas = conPeriodo
    ? `${formatoDiaCorto(fechas.from)} – ${formatoDiaCorto(fechas.to)}`
    : "Hoy";

  // "Ver renta" va al final para que los otros botones queden en la misma posición en todas las filas
  function accion(row: EquipoEstado) {
    return (
      <span className="acciones">
        <BotonProximamente texto="Ver detalles" icono="arrow_forward" />
        <BotonProximamente texto="Opciones" icono="expand_more" />
        {row.rentaId !== null && (
          <Button
            type="button"
            variant="primary"
            className="btn-chico"
            onClick={() => navigate(`/rentas/${row.rentaId}`)}
          >
            Ver renta {row.rentaFolio}
            <span className="material-symbols-rounded" aria-hidden="true">arrow_forward</span>
          </Button>
        )}
      </span>
    );
  }

  const columnas: TableColumn<EquipoEstado>[] = [
    {
      header: "Equipo",
      accessor: (row) => (
        <>
          <span className="celda-principal">{row.equipo}</span>
          <span className="celda-secundaria">{row.numeroUnidad}</span>
        </>
      ),
    },
    { header: "Categoría", accessor: (row) => row.categoria },
    { header: "Estado", accessor: (row) => <ChipEstadoEquipo estado={row.estado} /> },
    { header: "Acciones", accessor: accion, className: "celda-accion" },
  ];

  return (
    <div className="equipos-estados">
      <div className="pagina operacion">
        {/* Título */}
        <div className="equipos-encabezado">
          <div className="operacion-titulo">
            <h1>Equipos</h1>
            <p>Consulta y filtra los equipos registrados.</p>
          </div>
          <span title={PROXIMAMENTE}>
            <Button type="button" variant="primary" disabled>
              <span className="material-symbols-rounded" aria-hidden="true">add</span>
              Registrar equipo
            </Button>
          </span>
        </div>

        {error && <Aviso tipo="error" titulo={error} />}

        {/* Fechas y categoría */}
        <div className="operacion-filtros-izquierda">
          <div className="operacion-filtro">
            <span className="operacion-filtro-etiqueta">Fecha:</span>
            <DateRangePicker
              value={fechas}
              onChange={(campo, valor) => setFechas((prev) => ({ ...prev, [campo]: valor }))}
            />
          </div>
          <div className="operacion-filtro">
            <span className="operacion-filtro-etiqueta">Categoría:</span>
            <FilterDropdown
              options={categorias.map((c) => ({ label: c.nombre, value: c.nombre }))}
              selectedValue={categoria}
              allLabel="Todas"
              onChange={setCategoria}
            />
          </div>
        </div>

        {/* Estado */}
        <div className="operacion-filtro">
          <span className="operacion-filtro-etiqueta">Filtrar por:</span>
          <FilterRow
            options={filtrosEstado}
            selectedValue={estado}
            allLabel="Todos los equipos"
            allCount={deLaCategoria.length}
            onChange={setEstado}
          />
        </div>

        {periodoIncompleto && (
          <p className="equipos-ayuda">Elige las dos fechas para ver el estado en ese periodo.</p>
        )}

        <DataTable
          columns={columnas}
          data={visibles}
          getRowKey={(row) => row.id}
          emptyMessage={cargando ? "Cargando equipos…" : "No hay equipos con estos filtros."}
        />
      </div>

      {/* Resumen */}
      <aside className="equipos-resumen" aria-labelledby="titulo-resumen-equipos">
        <div className="equipos-resumen-encabezado">
          <h2 id="titulo-resumen-equipos">Resumen</h2>
        </div>
        <div className="equipos-resumen-fechas">
          <span className="material-symbols-rounded" aria-hidden="true">schedule</span>
          Fechas: {textoFechas}
        </div>
        {ESTADOS.map((valor) => (
          <div key={valor} className="equipos-conteo">
            <span className="equipos-conteo-numero">{cargando ? "–" : contar(valor)}</span>
            <span className="equipos-conteo-texto">
              <span className={`equipos-punto ${CLASE_ESTADO[valor]}`} aria-hidden="true" />
              {NOMBRE_ESTADO[valor]}
            </span>
          </div>
        ))}
      </aside>
    </div>
  );
}