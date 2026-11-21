import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Chip from "../../components/chips";
import FilterFactory from "../../components/filters/filterFactory";
import type { FilterDefinition } from "../../components/filters/filterTypes";
import DataTable, { type TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { listarPersonal } from "./personalApi";
import type { Personal } from "./personalTypes";
import { ICONO_TIPO, NOMBRE_TIPO, nombreCompleto } from "./personalTypes";
import "../../styles/personal/personal.css";

// Quita los acentos, para que "ramirez" encuentre "Ramírez"
const sinAcentos = (texto: string) => texto.normalize("NFD").replace(/[̀-ͯ]/g, "");

// Primero los activos y, dentro de cada grupo, por nombre
const ordenar = (lista: Personal[]) =>
  [...lista].sort(
    (a, b) => Number(b.activo) - Number(a.activo) || nombreCompleto(a).localeCompare(nombreCompleto(b), "es"),
  );

function filtros(personal: Personal[]): FilterDefinition<Personal>[] {
  const cuantos = (condicion: (p: Personal) => boolean) => personal.filter(condicion).length;
  return [
    {
      id: "busqueda",
      label: "Buscar personal",
      type: "text",
      placeholder: "Busca por nombre o código",
      getValue: (p) => {
        const texto = `${nombreCompleto(p)} ${p.codigo}`;
        return `${texto} ${sinAcentos(texto)}`;
      },
    },
    {
      id: "tipo",
      label: "Puesto",
      type: "row",
      options: [
        { label: NOMBRE_TIPO.OP_TRASLADO, value: "OP_TRASLADO", count: cuantos((p) => p.tipo === "OP_TRASLADO") },
        { label: NOMBRE_TIPO.MECANICO, value: "MECANICO", count: cuantos((p) => p.tipo === "MECANICO") },
      ],
      getValue: (p) => p.tipo,
    },
    {
      id: "estado",
      label: "Estado",
      type: "row",
      options: [
        { label: "Activos", value: "activo", count: cuantos((p) => p.activo) },
        { label: "De baja", value: "baja", count: cuantos((p) => !p.activo) },
      ],
      getValue: (p) => (p.activo ? "activo" : "baja"),
    },
  ];
}

// Aviso que deja Registrar o Editar al volver a la lista
interface EstadoNavegacion {
  aviso?: string;
}

/** Consultar personal (OHU-27): operadores de traslado y mecánicos registrados. */
export default function ConsultarPersonal() {
  useTitulo("Personal");
  const navigate = useNavigate();
  const ubicacion = useLocation();
  const [aviso, setAviso] = useState((ubicacion.state as EstadoNavegacion | null)?.aviso ?? "");

  const [personal, setPersonal] = useState<Personal[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    listarPersonal()
      .then((lista) => setPersonal(ordenar(lista)))
      .catch(() => setError("No fue posible cargar al personal."))
      .finally(() => setCargando(false));
  }, []);

  const columnas: TableColumn<Personal>[] = [
    {
      header: "Nombre",
      accessor: (p) => <span className="personal-nombre">{nombreCompleto(p)}</span>,
    },
    { header: "Código", accessor: (p) => <span className="personal-codigo">{p.codigo}</span> },
    {
      header: "Puesto",
      accessor: (p) => (
        <span className="personal-puesto">
          <span className="material-symbols-rounded" aria-hidden="true">
            {ICONO_TIPO[p.tipo]}
          </span>
          {NOMBRE_TIPO[p.tipo]}
        </span>
      ),
    },
    {
      header: "Estado",
      accessor: (p) =>
        p.activo ? (
          <Chip category="equipo" status="disponible">
            Activo
          </Chip>
        ) : (
          <Chip category="carta" status="borrador">
            De baja
          </Chip>
        ),
    },
    {
      header: "Acciones",
      className: "personal-acciones",
      accessor: (p) => (
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate(`/personal/${p.id}`)}
          aria-label={`Editar a ${nombreCompleto(p)}`}
        >
          <span className="material-symbols-rounded" aria-hidden="true">
            edit
          </span>
          Editar
        </Button>
      ),
    },
  ];

  return (
    <div className="pagina lista-personal">
      <div className="titulo-acciones">
        <div className="titulo-acciones-texto">
          <h1>Personal</h1>
          <p>Operadores de traslado y mecánicos. Cada quien entra a la app móvil con su código.</p>
        </div>
        <div className="titulo-acciones-botones">
          <Button type="button" variant="primary" onClick={() => navigate("/personal/registrar")}>
            <span className="material-symbols-rounded" aria-hidden="true">
              person_add
            </span>
            Registrar personal
          </Button>
        </div>
      </div>

      {aviso && <Aviso tipo="exito" titulo={aviso} onCerrar={() => setAviso("")} />}
      {error && <Aviso tipo="error" titulo={error} />}
      {cargando && <p>Cargando personal…</p>}

      {!cargando && !error && personal.length === 0 && (
        <div className="sin-resultados" role="status">
          <p>Aún no hay personal registrado.</p>
        </div>
      )}

      {!cargando && !error && personal.length > 0 && (
        <FilterFactory data={personal} filters={filtros(personal)}>
          {(filtrados, valores) => {
            const texto = valores.busqueda === "all" ? "" : (valores.busqueda ?? "").trim();
            return filtrados.length === 0 ? (
              <div className="sin-resultados" role="status">
                <p>{texto ? `No se encontró a nadie con “${texto}”.` : "No hay personal con esos filtros."}</p>
              </div>
            ) : (
              <div className="tabla-personal">
                <DataTable
                  columns={columnas}
                  data={filtrados}
                  getRowKey={(p) => p.id}
                  getRowClassName={(p) => (p.activo ? undefined : "personal-de-baja")}
                />
              </div>
            );
          }}
        </FilterFactory>
      )}
    </div>
  );
}
