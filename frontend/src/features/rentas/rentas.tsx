import { useEffect, useMemo, useState } from "react";
import { listarRentas } from "./rentasApi";
import type { Renta } from "./estadosRentaTypes";
import FilterFactory from "../../components/filters/filterFactory";
import type { FilterDefinition } from "../../components/filters/filterTypes";
import DataTable from "../../components/table";
import type { TableColumn } from "../../components/table";
import Chip from "../../components/chips";
import { Button } from "../../components/buttons";
import "../../styles/rentas/rentas.css";
import "../../styles/global.css";

import { useNavigate, useParams } from "react-router-dom";


interface RentaVista extends Renta {
  clienteNombre: string;
  equiposResumen: string;
  categoriaFiltro: string;
}

type EstadoFiltro =
  | "APARTADOS_PROXIMOS"
  | "POR_VENCER"
  | "PENDIENTES_RECOLECCION";

const DIAS_AVISO = 7;

function fechaLocal(fecha: Date): string {
  const year = fecha.getFullYear();
  const month = String(fecha.getMonth() + 1).padStart(2, "0");
  const day = String(fecha.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function diasEntre(fecha: string): number {
  const hoy = new Date(`${fechaLocal(new Date())}T00:00:00`);
  const objetivo = new Date(`${fecha}T00:00:00`);

  return Math.round(
    (objetivo.getTime() - hoy.getTime()) / 86400000
  );
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
    [
      cliente.nombre,
      cliente.apellidoPaterno,
      cliente.apellidoMaterno,
    ]
      .filter(Boolean)
      .join(" ")
  );
}

function obtenerCategoria(renta: Renta): EstadoFiltro | "TODAS" {
  const diasInicio = diasEntre(renta.fechaInicio);
  const diasFin = diasEntre(renta.fechaFin);

  const pendienteRecoleccion = renta.equipos.some((equipo) =>
    ["EN_OPERACION", "EN_REVISION_ENTRADA"].includes(equipo.fase)
  );

  if (pendienteRecoleccion) {
    return "PENDIENTES_RECOLECCION";
  }

  if (
    renta.estado === "EN_CURSO" &&
    diasFin >= 0 &&
    diasFin <= DIAS_AVISO
  ) {
    return "POR_VENCER";
  }

  if (
    renta.estado === "PROGRAMADA" &&
    diasInicio >= 0 &&
    diasInicio <= DIAS_AVISO
  ) {
    return "APARTADOS_PROXIMOS";
  }

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

export default function Rentas() {
  const navigate = useNavigate();
  const [rentas, setRentas] = useState<Renta[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function cargarRentas() {
      try {
        setLoading(true);
        const data = await listarRentas();
        setRentas(data);
      } catch (err) {
        console.error("Error al cargar rentas:", err);
        setError("No fue posible cargar las rentas.");
      } finally {
        setLoading(false);
      }
    }

    cargarRentas();
  }, []);

  const rentasVista = useMemo<RentaVista[]>(() => {
    return rentas.map((renta) => ({
      ...renta,
      clienteNombre: obtenerNombreCliente(renta),
      equiposResumen: renta.equipos
        .map((equipo) => equipo.numeroUnidad)
        .join(", "),
      categoriaFiltro: obtenerCategoria(renta),
    }));
  }, [rentas]);

  const filters = useMemo<FilterDefinition<RentaVista>[]>(() => [
    {
      id: "categoria",
      label: "Filtrar por",
      type: "row",
      options: [
        {
          label: "Apartados próximos",
          value: "APARTADOS_PROXIMOS",
        },
        {
          label: "Por vencer",
          value: "POR_VENCER",
        },
        {
          label: "Pendientes de recolección",
          value: "PENDIENTES_RECOLECCION",
        },
      ],
      getValue: renta => renta.categoriaFiltro,
    },
    {
      id: "cliente",
      label: "Cliente",
      type: "text",
      placeholder: "Buscar cliente...",
      getValue: renta => renta.clienteNombre,
    },
    {
      id: "periodo",
      label: "Periodo",
      type: "date-range",
      getValue: renta => renta.fechaInicio,
    },
  ], []);

  const columns = useMemo<TableColumn<RentaVista>[]>(() => [
    {
      header: "Renta",
      accessor: renta => (
        <div className="renta-cell-primary">
          <strong>{renta.clienteNombre}</strong>
          <small>{renta.folio}</small>
        </div>
      ),
    },
    {
      header: "Equipos",
      accessor: renta => (
        <div>
          <strong>{renta.equipos.length} equipos</strong>
          <br />
          <small>{renta.equiposResumen || "Sin equipos"}</small>
        </div>
      ),
    },
    {
      header: "Información general",
      accessor: renta => (
        <div className="renta-cell-info">
          <span>
            <strong>Inicia:</strong> {formatearFecha(renta.fechaInicio)}
          </span>
          <br />
          <span>
            <strong>Termina:</strong> {formatearFecha(renta.fechaFin)}
          </span>
        </div>
      ),
    },
    {
      header: "Estado",
      accessor: renta => (
        <Chip
          category="renta"
          status={obtenerEstadoChip(renta.estado)}
        />
      ),
    },
    {
      header: "Acciones",
      accessor: renta => (
        <Button
          variant="secondary"
          onClick={() => navigate(`/rentas/${renta.id}`)}
        >
          Ver renta
          <span className="material-symbols-rounded">
            arrow_forward
          </span>
        </Button>
      ),
      className: "cell-actions",
    },
  ], []);

  const programadas = rentas.filter(
    renta => renta.estado === "PROGRAMADA"
  ).length;

  const pendientesRecoleccion = rentas.filter(renta =>
    renta.equipos.some(equipo =>
      ["EN_OPERACION", "EN_REVISION_ENTRADA"].includes(equipo.fase)
    )
  ).length;

  const porVencer = rentas.filter(renta => {
    const dias = diasEntre(renta.fechaFin);

    return (
      renta.estado === "EN_CURSO" &&
      dias >= 0 &&
      dias <= DIAS_AVISO
    );
  }).length;

  if (loading) return <p>Cargando rentas...</p>;
  if (error) return <p>{error}</p>;

  return (
    <div className="rentas-layout">

      <main className="rentas-page">
        <section className="rentas-content">

          <header className="rentas-header">
            <h1 className="title-heading">Consultar rentas actuales</h1>
          </header>

          <FilterFactory
            data={rentasVista}
            filters={filters}
            allLabel="Todas"
          >
            {(filteredRentas) => (
              <DataTable
                columns={columns}
                data={filteredRentas}
                getRowKey={renta => renta.id}
              />
            )}
          </FilterFactory>

        </section>
      </main>

      <aside className="rentas-summary">
        <h2 className="title-heading">Resumen</h2>
        <p>◷ Fechas: Todas</p>

        <div className="summary-item">
          <strong>{programadas}</strong>
          <span>Rentas<br />programadas</span>
        </div>

        <div className="summary-item">
          <strong>{pendientesRecoleccion}</strong>
          <span>Recolecciones<br />pendientes</span>
        </div>

        <div className="summary-item">
          <strong>{porVencer}</strong>
          <span>Por vencer</span>
        </div>
      </aside>

    </div>
  );
}
