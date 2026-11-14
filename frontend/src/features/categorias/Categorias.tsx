import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import DataTable, { type TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { listarCategorias, type Categoria } from "./categoriasApi";
import "../../styles/categorias/categorias.css";

const columnas: TableColumn<Categoria>[] = [
  { header: "Nombre", accessor: (c) => c.nombre },
  {
    header: "Acciones",
    className: "categorias-acciones",
    accessor: (c) => (
      <Link to={`/categorias/${c.id}/editar`} className="categorias-editar">
        Editar
      </Link>
    ),
  },
];

export default function Categorias() {
  useTitulo("Categorías");
  const navigate = useNavigate();
  const location = useLocation();
  const aviso: string | undefined = location.state?.aviso;

  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    listarCategorias()
      .then((datos) => setCategorias([...datos].sort((a, b) => a.id - b.id)))
      .catch(() => setError("No fue posible cargar las categorías."))
      .finally(() => setCargando(false));
  }, []);

  const botonNueva = (
    <Button
      type="button"
      variant="primary"
      onClick={() => navigate("/categorias/nueva")}
    >
      <span className="material-symbols-rounded" aria-hidden="true">
        add
      </span>
      Nueva categoría
    </Button>
  );

  return (
    <div className="pagina categorias">
      {aviso && (
        <Aviso
          tipo="exito"
          titulo={aviso}
          onCerrar={() => navigate(location.pathname, { replace: true })}
        />
      )}

      <div className="titulo-acciones">
        <div className="titulo-acciones-texto">
          <h1>Categorías</h1>
          <p>Agrupa los equipos para buscar su disponibilidad por categoría.</p>
        </div>
        {categorias.length > 0 && (
          <div className="titulo-acciones-botones">{botonNueva}</div>
        )}
      </div>

      {error && <Aviso tipo="error" titulo={error} />}

      {!cargando && !error && categorias.length === 0 && (
        <div className="sin-resultados" role="status">
          <p>Aún no hay categorías</p>
          {botonNueva}
        </div>
      )}

      {categorias.length > 0 && (
        <>
          <div className="categorias-tabla">
            <DataTable
              columns={columnas}
              data={categorias}
              getRowKey={(c) => c.id}
            />
          </div>

          <ul className="categorias-tarjetas">
            {categorias.map((c) => (
              <li key={c.id} className="categoria-tarjeta">
                <strong>{c.nombre}</strong>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => navigate(`/categorias/${c.id}/editar`)}
                >
                  Editar
                </Button>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
