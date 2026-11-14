import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import DataTable, { type TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { listarCategorias, type Categoria } from "./categoriasApi";
import ModalCategoria from "./ModalCategoria";
import "../../styles/categorias/categorias.css";

type Formulario = { categoria: Categoria | null };

const AVISOS = {
  creada: { titulo: "Categoría creada", detalle: "Ya aparece al final de la lista." },
  actualizada: { titulo: "Categoría actualizada", detalle: "El cambio ya se ve en la lista." },
};

export default function Categorias() {
  useTitulo("Categorías");

  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [formulario, setFormulario] = useState<Formulario | null>(null);
  const [aviso, setAviso] = useState<keyof typeof AVISOS | null>(null);

  useEffect(() => {
    listarCategorias()
      .then((datos) => setCategorias([...datos].sort((a, b) => a.id - b.id)))
      .catch(() => setError("No fue posible cargar las categorías."))
      .finally(() => setCargando(false));
  }, []);

  const abrir = (categoria: Categoria | null) => {
    setAviso(null);
    setFormulario({ categoria });
  };

  const alGuardar = (guardada: Categoria) => {
    const existe = categorias.some((c) => c.id === guardada.id);
    setCategorias(
      existe
        ? categorias.map((c) => (c.id === guardada.id ? guardada : c))
        : [...categorias, guardada],
    );
    setAviso(existe ? "actualizada" : "creada");
    setFormulario(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const botonEditar = (c: Categoria) => (
    <Button
      type="button"
      variant="secondary"
      className="categorias-editar"
      onClick={() => abrir(c)}
    >
      <span className="material-symbols-rounded" aria-hidden="true">
        edit
      </span>
      Editar
    </Button>
  );

  const columnas: TableColumn<Categoria>[] = [
    { header: "Nombre", accessor: (c) => c.nombre, className: "categorias-nombre" },
    { header: "Acciones", accessor: botonEditar, className: "categorias-acciones" },
  ];

  return (
    <div className="pagina categorias">
      {aviso && (
        <Aviso
          tipo="exito"
          titulo={AVISOS[aviso].titulo}
          onCerrar={() => setAviso(null)}
        >
          {AVISOS[aviso].detalle}
        </Aviso>
      )}

      <Link to="/equipos" className="categorias-volver">
        <span className="material-symbols-rounded" aria-hidden="true">
          arrow_back
        </span>
        Volver a equipos
      </Link>

      <div className="titulo-acciones">
        <div className="titulo-acciones-texto">
          <h1>Categorías</h1>
          <p>Administra las categorías de modelos en el sistema.</p>
        </div>
        <div className="titulo-acciones-botones">
          <Button type="button" variant="primary" onClick={() => abrir(null)}>
            + Nueva categoría
          </Button>
        </div>
      </div>

      {error && <Aviso tipo="error" titulo={error} />}

      {!cargando && !error && categorias.length === 0 && (
        <div className="categorias-vacio" role="status">
          <span className="material-symbols-rounded" aria-hidden="true">
            sell
          </span>
          <strong>Aún no hay categorías</strong>
          <p>Crea la primera para agrupar tus modelos de equipos.</p>
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
                {botonEditar(c)}
              </li>
            ))}
          </ul>
        </>
      )}

      {formulario && (
        <ModalCategoria
          categoria={formulario.categoria}
          onCerrar={() => setFormulario(null)}
          onGuardada={alGuardar}
        />
      )}
    </div>
  );
}
