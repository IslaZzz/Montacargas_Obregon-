import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../components/buttons";
import DataTable, { type TableColumn } from "../../components/table";
import FilterFactory from "../../components/filters/filterFactory";
import type { FilterDefinition } from "../../components/filters/filterTypes";
import { buscarClientes } from "./clientesApi";
import type { Cliente } from "./clientesTypes";

// Cuántos clientes se muestran por tanda
const POR_PAGINA = 20;

// 6871234567 pasa a 687 123 4567
const formatearTelefono = (telefono: string) =>
  telefono.replace(/(\d{3})(\d{3})(\d{4})/, "$1 $2 $3");

// Quita los acentos, para que "gutierrez" encuentre "Gutiérrez"
const sinAcentos = (texto: string) =>
  texto.normalize("NFD").replace(/[\u0300-\u036f]/g, "");

// Un solo buscador que revisa nombre, empresa, teléfono y RFC a la vez
const filtros: FilterDefinition<Cliente>[] = [
  {
    id: "busqueda",
    label: "Buscar cliente",
    type: "text",
    placeholder: "Busca por nombre, empresa, RFC o teléfono",
    getValue: (c) => {
      const texto = [
        c.nombre,
        c.apellidoPaterno,
        c.apellidoMaterno,
        c.empresa,
        c.telefono,
        formatearTelefono(c.telefono),
        ...c.razonesSociales.map((r) => r.rfc),
      ]
        .filter(Boolean)
        .join(" ");
      // Se agrega también la versión sin acentos
      return `${texto} ${sinAcentos(texto)}`;
    },
  },
];

// Tabla con límite de 20, "Ver más" y mensaje sin resultados
interface TablaClientesProps {
  datos: Cliente[];
  columnas: TableColumn<Cliente>[];
  hayClientes: boolean;
  textoBuscado?: string;
}

function TablaClientes({
  datos,
  columnas,
  hayClientes,
  textoBuscado = "",
}: TablaClientesProps) {
  const [visibles, setVisibles] = useState(POR_PAGINA);

  // Cuando cambia el filtro, se vuelve a las primeras 20
  useEffect(() => {
    setVisibles(POR_PAGINA);
  }, [datos]);

  // Sin resultados
  if (datos.length === 0) {
    return (
      <div className="sin-resultados" role="status">
        <p>
          {textoBuscado
            ? `No se encontró ningún cliente con “${textoBuscado}”`
            : hayClientes
              ? "No se encontró ningún cliente"
              : "Aún no hay clientes registrados"}
        </p>
      </div>
    );
  }

  const mostrados = datos.slice(0, visibles);

  return (
    <>
      <div className="tabla-clientes">
        <DataTable
          columns={columnas}
          data={mostrados}
          getRowKey={(c) => c.id}
        />
      </div>

      {/* Si hay más de los que se muestran */}
      {datos.length > mostrados.length && (
        <div className="ver-mas">
          <Button
            type="button"
            variant="tertiary"
            onClick={() => setVisibles(visibles + POR_PAGINA)}
          >
            Ver más
          </Button>
        </div>
      )}
    </>
  );
}

export default function BuscarClientes() {
  const navigate = useNavigate();

  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    // Se ejecuta una vez al abrir la pantalla
    buscarClientes()
      .then(setClientes)
      .catch(() => setError("No fue posible cargar los clientes."))
      .finally(() => setCargando(false));
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
          {/*Botón para ver datos del cliente*/}
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate(`/clientes/${c.id}`)}
          >
            Ver datos
          </Button>
          {/*Botón para crear cotización, pasa el cliente a la pantalla de
          nueva cotización*/}
          <Button
            type="button"
            variant="secondary"
            onClick={() =>
              navigate("/cotizaciones/nueva", { state: { cliente: c } })
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

      {/* Buscador y tabla, filtra mientras se escribe */}
      {!cargando && !error && (
        <FilterFactory data={clientes} filters={filtros}>
          {(filtrados, valores) => (
            <TablaClientes
              datos={filtrados}
              columnas={columnas}
              hayClientes={clientes.length > 0}
              // Sin texto el filtro vale "all"
              textoBuscado={valores.busqueda === "all" ? "" : (valores.busqueda ?? "").trim()}
            />
          )}
        </FilterFactory>
      )}
    </div>
  );
}
