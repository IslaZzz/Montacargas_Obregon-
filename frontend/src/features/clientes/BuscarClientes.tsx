import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../components/buttons";
import DataTable, { type TableColumn } from "../../components/table";
import FilterFactory from "../../components/filters/filterFactory";
import type { FilterDefinition } from "../../components/filters/filterTypes";
import { buscarClientes } from "./clientesApi";
import type { Cliente } from "./clientesTypes";
import { useTitulo } from "../../hooks/useTitulo";

// 6871234567 pasa a 687 123 4567
const formatearTelefono = (telefono: string) =>
  telefono.replace(/(\d{3})(\d{3})(\d{4})/, "$1 $2 $3");

// Un solo buscador que revisa nombre, empresa, teléfono y RFC a la vez
const filtros: FilterDefinition<Cliente>[] = [
  {
    id: "busqueda",
    label: "Buscar cliente",
    type: "text",
    placeholder: "Busca por nombre, empresa, RFC o teléfono",
    getValue: (c) =>
      [
        c.nombre,
        c.apellidoPaterno,
        c.apellidoMaterno,
        c.empresa,
        c.telefono,
        formatearTelefono(c.telefono),
        ...c.razonesSociales.map((r) => r.rfc),
      ]
        .filter(Boolean)
        .join(" "),
  },
];

export default function BuscarClientes() {
  useTitulo("Clientes");
  const navigate = useNavigate();

  // El estado es una lista de Cliente, y empieza vacía
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    // Se ejecuta una vez al abrir la pantalla
    buscarClientes()
      .then(setClientes)
      .catch(() => setError("No fue posible cargar los clientes."));
  }, []);

  // Columnas de la tabla
  const columnas: TableColumn<Cliente>[] = [
    {
      header: "Cliente",
      accessor: (c) => (
        <>
          <span className="cliente-nombre">
            {[c.nombre, c.apellidoPaterno, c.apellidoMaterno]
              .filter(Boolean)
              .join(" ")}
          </span>
          <span className="cliente-empresa">
            {c.empresa || "Persona física"}
          </span>
        </>
      ),
    },
    { header: "Teléfono", accessor: (c) => formatearTelefono(c.telefono) },
    { header: "RFC", accessor: (c) => c.razonesSociales[0]?.rfc ?? "—" },
    { header: "Razones sociales", accessor: (c) => c.razonesSociales.length },
    {
      header: "Acciones",
      accessor: (c) => (
        <div className="acciones-fila">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate(`/clientes/${c.id}`)}
          >
            Ver datos
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() =>
              navigate("/cotizaciones/nueva", { state: { clienteId: c.id } })
            }
          >
            <span className="material-symbols-rounded">description</span>
            Crear cotización
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="pagina lista-clientes">
      {/* Título y botón de registrar */}
      <div className="titulo-acciones">
        <div className="titulo-acciones-texto">
          <h1>Clientes</h1>
          <p>Busca un cliente para ver sus datos o crear una cotización.</p>
        </div>
        <div className="titulo-acciones-botones">
          <Button
            type="button"
            variant="primary"
            onClick={() => navigate("/clientes/registrar")}
          >
            <span className="material-symbols-rounded">add</span>
            Registrar cliente
          </Button>
        </div>
      </div>

      {error && (
        <div className="aviso-error" role="alert">
          <span className="material-symbols-rounded">error</span>
          <strong>{error}</strong>
        </div>
      )}

      {/* Buscador y tabla */}
      <FilterFactory data={clientes} filters={filtros}>
        {(filtrados) => (
          <div className="tabla-clientes">
            <DataTable
              columns={columnas}
              data={filtrados}
              getRowKey={(c) => c.id}
              emptyMessage="No se encontraron clientes"
            />
          </div>
        )}
      </FilterFactory>
    </div>
  );
}
