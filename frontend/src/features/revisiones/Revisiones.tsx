import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import AsignarMecanicoModal from "../../components/AsignarMecanicoModal";
import type { ElementoAsignar, MecanicoOpcion } from "../../components/AsignarMecanicoModal";
import Aviso from "../../components/Aviso";
import Avatar from "../../components/Avatar";
import { Button } from "../../components/buttons";
import Chip from "../../components/chips";
import FilterRow from "../../components/filters/filterRow";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { formatoDiaCorto, formatoHora } from "../../utils/fechas";
import { asignarRevisiones, listarMecanicosRevision, listarRevisiones } from "./revisionesApi";
import type { Revision, RevisionesRenta } from "./revisionesTypes";
import {
  ESTADOS,
  TIPOS,
  chipDeEstado,
  chipDeResultado,
  chipDeTipo,
  esAsignable,
  etiquetaDeEstado,
  mecanicoComun,
  mecanicosDelGrupo,
} from "./revisionesUtils";
import "../../styles/revisiones/revisiones.css";

// Lo que se está asignando en el modal: la renta completa o una sola revisión
interface Asignacion {
  grupo: RevisionesRenta;
  revisiones: Revision[];
}

export default function Revisiones() {
  useTitulo("Revisiones");

  const [grupos, setGrupos] = useState<RevisionesRenta[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  // Filtros: "all" muestra todo
  const [estado, setEstado] = useState("all");
  const [tipo, setTipo] = useState("all");

  // Rentas desplegadas, por su clave
  const [abiertas, setAbiertas] = useState<Set<string>>(new Set());

  // Lo que se está asignando; null = modal cerrado
  const [enModal, setEnModal] = useState<Asignacion | null>(null);

  // Confirmación después de asignar y fila que se resalta
  const [exito, setExito] = useState<string | null>(null);
  const [resaltadas, setResaltadas] = useState<Set<string>>(new Set());

  useEffect(() => {
    // Se ejecuta una vez al abrir la pantalla
    listarRevisiones()
      .then(setGrupos)
      .catch(() => setError("No fue posible cargar las revisiones."))
      .finally(() => setCargando(false));
  }, []);

  const visibles = grupos.filter(
    (g) => (estado === "all" || g.estado === estado) && (tipo === "all" || g.tipo === tipo),
  );

  // Un chip por estado y por tipo, con cuántas rentas tiene cada uno
  const filtrosEstado = ESTADOS.map((e) => ({
    label: e.etiqueta,
    value: e.valor,
    count: grupos.filter((g) => g.estado === e.valor).length,
  }));
  const filtrosTipo = TIPOS.map((t) => ({
    label: t.etiqueta,
    value: t.valor,
    count: grupos.filter((g) => g.tipo === t.valor).length,
  }));

  function alternar(clave: string) {
    setAbiertas((anterior) => {
      const nuevas = new Set(anterior);
      if (nuevas.has(clave)) nuevas.delete(clave);
      else nuevas.add(clave);
      return nuevas;
    });
  }

  // La renta completa: solo las revisiones que todavía se pueden asignar
  function abrirParaRenta(g: RevisionesRenta) {
    setExito(null);
    setEnModal({ grupo: g, revisiones: g.revisiones.filter(esAsignable) });
  }

  function abrirParaRevision(g: RevisionesRenta, r: Revision) {
    setExito(null);
    setEnModal({ grupo: g, revisiones: [r] });
  }

  // Si el backend responde error, el modal lo muestra y no se cierra
  async function confirmar(mecanico: MecanicoOpcion) {
    if (!enModal) return;
    const lista = await asignarRevisiones({
      revisionIds: enModal.revisiones.map((r) => r.id),
      personalId: mecanico.id,
    });

    setGrupos(lista);
    const n = enModal.revisiones.length;
    setExito(
      n === 1
        ? `La revisión de ${enModal.revisiones[0].numeroUnidad} quedó asignada a ${mecanico.nombre}`
        : `Las ${n} revisiones de la renta ${enModal.grupo.folioRenta} quedaron asignadas a ${mecanico.nombre}`,
    );
    setResaltadas(new Set([enModal.grupo.clave]));
    setEnModal(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function accionRenta(g: RevisionesRenta) {
    // Si todas están terminadas o canceladas, no hay nada que asignar
    if (!g.revisiones.some(esAsignable)) return null;
    const pendiente = g.estado === "PENDIENTE";
    return (
      <Button
        type="button"
        variant={pendiente ? "primary" : "tertiary"}
        className="btn-chico"
        onClick={() => abrirParaRenta(g)}
      >
        <span className="material-symbols-rounded" aria-hidden="true">
          {pendiente ? "person_add" : "swap_horiz"}
        </span>
        {pendiente ? "Asignar" : "Cambiar"}
      </Button>
    );
  }

  // Los equipos de la renta, cuando está desplegada
  function detalle(g: RevisionesRenta): ReactNode {
    if (!abiertas.has(g.clave)) return null;
    return (
      <ul className="revisiones-equipos">
        {g.revisiones.map((r) => (
          <li key={r.id}>
            <div>
              <span className="celda-principal">{r.numeroUnidad}</span>
              <span className="celda-secundaria">{r.descripcionEquipo}</span>
            </div>

            <div className="revisiones-equipo-mecanico">
              {r.mecanicoNombre ? (
                <>
                  <Avatar nombre={r.mecanicoNombre} />
                  {r.mecanicoNombre}
                </>
              ) : (
                <span className="celda-secundaria">Sin mecánico</span>
              )}
            </div>

            <div className="revisiones-equipo-estado">
              <Chip category="revision" status={chipDeEstado(r.estado)}>
                {etiquetaDeEstado(r.estado)}
              </Chip>
              {r.resultado && (
                <Chip category="revision" status={chipDeResultado[r.resultado]} />
              )}
            </div>

            <div className="revisiones-equipo-notas">{r.notas}</div>

            <div className="celda-accion">
              {esAsignable(r) && (
                <Button
                  type="button"
                  variant="tertiary"
                  className="btn-chico"
                  onClick={() => abrirParaRevision(g, r)}
                >
                  {r.mecanicoId ? "Cambiar" : "Asignar"}
                </Button>
              )}
            </div>
          </li>
        ))}
      </ul>
    );
  }

  const columnas: TableColumn<RevisionesRenta>[] = [
    {
      header: "",
      accessor: (g) => (
        <button
          type="button"
          className="revisiones-desplegar"
          aria-expanded={abiertas.has(g.clave)}
          aria-label={`${abiertas.has(g.clave) ? "Ocultar" : "Ver"} los equipos de ${g.folioRenta}`}
          onClick={() => alternar(g.clave)}
        >
          <span className="material-symbols-rounded" aria-hidden="true">
            {abiertas.has(g.clave) ? "expand_less" : "expand_more"}
          </span>
        </button>
      ),
      className: "celda-desplegar",
    },
    {
      header: "Renta",
      accessor: (g) => (
        <>
          <span className="celda-principal">{g.folioRenta}</span>
          <span className="celda-secundaria">{g.cliente}</span>
        </>
      ),
    },
    {
      header: "Tipo",
      accessor: (g) => <Chip category="revision" status={chipDeTipo(g.tipo)} />,
    },
    {
      header: "Programada",
      accessor: (g) => (
        <>
          <span className="celda-principal">{formatoDiaCorto(g.fechaProgramada)}</span>
          <span className="celda-secundaria">{formatoHora(g.fechaProgramada)}</span>
        </>
      ),
    },
    {
      header: "Avance",
      accessor: (g) => <Avance revisadas={g.revisadas} total={g.total} />,
    },
    {
      header: "Mecánico",
      accessor: (g) => <CeldaMecanico nombres={mecanicosDelGrupo(g)} />,
    },
    {
      header: "Estado",
      accessor: (g) => (
        <Chip category="revision" status={chipDeEstado(g.estado)}>
          {etiquetaDeEstado(g.estado)}
        </Chip>
      ),
    },
    { header: "Acción", accessor: accionRenta, className: "celda-accion" },
  ];

  // Datos del modal
  const n = enModal?.revisiones.length ?? 0;
  const actual = enModal ? mecanicoComun(enModal.revisiones) : null;
  const verbo = actual !== null ? "Cambiar el mecánico de" : "Asignar mecánico a";
  const titulo = !enModal
    ? ""
    : n === 1
      ? `${verbo} ${enModal.revisiones[0].numeroUnidad}.`
      : `${verbo} la renta ${enModal.grupo.folioRenta}.`;
  const subtitulo = enModal
    ? `Revisión de ${enModal.grupo.tipo === "SALIDA" ? "salida" : "regreso"}, ` +
      `programada para el ${formatoDiaCorto(enModal.grupo.fechaProgramada).toLowerCase()} ` +
      `a las ${formatoHora(enModal.grupo.fechaProgramada)}.`
    : undefined;
  const textoBoton =
    actual !== null ? "Cambiar mecánico" : `Asignar ${n} ${n === 1 ? "equipo" : "equipos"}`;
  const elementos: ElementoAsignar[] =
    enModal?.revisiones.map((r) => ({
      id: r.id,
      titulo: r.numeroUnidad,
      descripcion: r.descripcionEquipo,
      detalle: r.mecanicoNombre ? `Ahora: ${r.mecanicoNombre}` : "Sin mecánico",
    })) ?? [];

  return (
    <div className="pagina revisiones">
      {/* Título */}
      <div className="revisiones-titulo">
        <h1>Revisiones</h1>
        <p>Asigna a un mecánico la revisión de los equipos de cada renta.</p>
      </div>

      {/* Avisos */}
      {exito && (
        <Aviso tipo="exito" titulo={exito} onCerrar={() => setExito(null)}>
          Te la marcamos en la tabla.
        </Aviso>
      )}
      {error && (
        <Aviso tipo="error" titulo={error}>
          Revisa tu conexión y vuelve a abrir la pantalla.
        </Aviso>
      )}

      {/* Filtros */}
      <div className="revisiones-filtros">
        <div className="revisiones-filtro">
          <span className="revisiones-filtro-etiqueta">Mostrar:</span>
          <FilterRow
            options={filtrosEstado}
            selectedValue={estado}
            allLabel="Todas"
            allCount={grupos.length}
            onChange={setEstado}
          />
        </div>
        <div className="revisiones-filtro">
          <span className="revisiones-filtro-etiqueta">Tipo:</span>
          <FilterRow
            options={filtrosTipo}
            selectedValue={tipo}
            allLabel="Todas"
            onChange={setTipo}
          />
        </div>
      </div>

      {/* Tabla: clic en una fila para ver sus equipos */}
      <DataTable
        columns={columnas}
        data={visibles}
        getRowKey={(g) => g.clave}
        getRowClassName={(g) => (resaltadas.has(g.clave) ? "fila-resaltada" : undefined)}
        onRowClick={(g) => alternar(g.clave)}
        renderDetalle={detalle}
        emptyMessage={cargando ? "Cargando revisiones…" : "No hay revisiones con estos filtros."}
      />

      <AsignarMecanicoModal
        abierto={enModal !== null}
        titulo={titulo}
        subtitulo={subtitulo}
        elementos={elementos}
        mecanicoActualId={actual}
        textoBoton={textoBoton}
        cargarMecanicos={listarMecanicosRevision}
        onConfirmar={confirmar}
        onCerrar={() => setEnModal(null)}
      />
    </div>
  );
}

// "2 de 3 revisados", con una barra
function Avance({ revisadas, total }: { revisadas: number; total: number }) {
  const porcentaje = total === 0 ? 0 : Math.round((revisadas / total) * 100);
  return (
    <div className="revisiones-avance">
      <span>
        {revisadas} de {total} {total === 1 ? "revisado" : "revisados"}
      </span>
      <span className="revisiones-avance-barra" aria-hidden="true">
        <span style={{ width: `${porcentaje}%` }} />
      </span>
    </div>
  );
}

// Uno solo con su nombre, varios como "2 mecánicos", ninguno como "Sin asignar"
function CeldaMecanico({ nombres }: { nombres: string[] }) {
  if (nombres.length === 0) {
    return <Chip category="revision" status="sin-asignar" />;
  }
  if (nombres.length === 1) {
    return (
      <div className="celda-mecanico">
        <Avatar nombre={nombres[0]} />
        <span className="celda-mecanico-nombre">{nombres[0]}</span>
      </div>
    );
  }
  return <span className="celda-secundaria">{nombres.length} mecánicos</span>;
}