import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import DataTable, { type TableColumn } from "../../components/table";
import { useTitulo } from "../../hooks/useTitulo";
import { listarModelos } from "./modelosApi";
import type { Modelo } from "./modelosTypes";
import "../../styles/modelos/modelos.css";

export type AvisoModelo = "creado" | "actualizado";

const AVISOS: Record<AvisoModelo, { titulo: string; detalle: string }> = {
  creado: {
    titulo: "Modelo creado",
    detalle: "Ya aparece al final de la lista. Su tarifa se captura en «Tarifas».",
  },
  actualizado: {
    titulo: "Modelo actualizado",
    detalle: "Las unidades de este modelo ya muestran los datos nuevos.",
  },
};

export default function Modelos() {
  useTitulo("Modelos");
  const navigate = useNavigate();
  const location = useLocation();

  const [modelos, setModelos] = useState<Modelo[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  // El formulario regresa aquí con el aviso que toca mostrar
  const [aviso, setAviso] = useState<AvisoModelo | null>(
    (location.state as { aviso?: AvisoModelo } | null)?.aviso ?? null,
  );

  // Quita el aviso del historial para que no vuelva a salir al recargar
  useEffect(() => {
    if (location.state) navigate(location.pathname, { replace: true, state: null });
  }, [location, navigate]);

  useEffect(() => {
    listarModelos()
      .then(setModelos)
      .catch(() => setError("No fue posible cargar los modelos."))
      .finally(() => setCargando(false));
  }, []);

  const botonEditar = (m: Modelo) => (
    <Button
      type="button"
      variant="secondary"
      className="modelos-editar"
      onClick={() => navigate(`/modelos/${m.id}/editar`)}
    >
      <span className="material-symbols-rounded" aria-hidden="true">
        edit
      </span>
      Editar
    </Button>
  );

  const columnas: TableColumn<Modelo>[] = [
    { header: "Nombre", accessor: (m) => m.nombre, className: "modelos-dato" },
    { header: "Categoría", accessor: (m) => m.categoria, className: "modelos-dato" },
    { header: "Marca y modelo", accessor: (m) => m.marcaModelo, className: "modelos-dato" },
    { header: "Acciones", accessor: botonEditar, className: "modelos-acciones" },
  ];

  return (
    <div className="pagina modelos">
      {aviso && (
        <Aviso tipo="exito" titulo={AVISOS[aviso].titulo} onCerrar={() => setAviso(null)}>
          {AVISOS[aviso].detalle}
        </Aviso>
      )}

      <div className="titulo-acciones">
        <div className="titulo-acciones-texto">
          <h1>Modelos</h1>
          <p>Registra y edita los modelos de equipo. Sus unidades comparten estos datos.</p>
        </div>
        <div className="titulo-acciones-botones">
          <Button type="button" variant="primary" onClick={() => navigate("/modelos/nuevo")}>
            + Nuevo modelo
          </Button>
        </div>
      </div>

      {error && <Aviso tipo="error" titulo={error} />}

      {!cargando && !error && modelos.length === 0 && (
        <div className="modelos-vacio" role="status">
          <span className="material-symbols-rounded" aria-hidden="true">
            sell
          </span>
          <strong>Aún no hay modelos</strong>
          <p>Registra el primero para dar de alta sus unidades.</p>
        </div>
      )}

      {modelos.length > 0 && (
        <>
          <div className="modelos-tabla">
            <DataTable columns={columnas} data={modelos} getRowKey={(m) => m.id} />
          </div>

          <ul className="modelos-tarjetas">
            {modelos.map((m) => (
              <li key={m.id} className="modelo-tarjeta">
                <div>
                  <strong>{m.nombre}</strong>
                  <span>
                    {m.categoria} · {m.marcaModelo}
                  </span>
                </div>
                {botonEditar(m)}
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
