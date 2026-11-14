import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Campo from "../../components/CampoForm";
import { useTitulo } from "../../hooks/useTitulo";
import { ErrorApi } from "../../services/apiClient";
import {
  actualizarCategoria,
  crearCategoria,
  obtenerCategoria,
} from "./categoriasApi";
import "../../styles/categorias/categorias.css";

export default function FormCategoria() {
  const { id } = useParams();
  const editando = id !== undefined;
  const titulo = editando ? "Editar categoría" : "Nueva categoría";
  useTitulo(titulo);
  const navigate = useNavigate();

  const [nombre, setNombre] = useState("");
  const [errorNombre, setErrorNombre] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [cargada, setCargada] = useState(!editando);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!editando) return;
    obtenerCategoria(Number(id))
      .then((c) => {
        setNombre(c.nombre);
        setCargada(true);
      })
      .catch(() => setMensaje("No fue posible cargar la categoría."));
  }, [editando, id]);

  const cambiarNombre = (valor: string) => {
    setNombre(valor);
    setErrorNombre("");
  };

  async function guardar(e: FormEvent) {
    e.preventDefault();
    setMensaje("");

    const limpio = nombre.trim();
    if (!limpio) {
      setErrorNombre("Captura el nombre");
      return;
    }

    setGuardando(true);
    try {
      if (editando) {
        await actualizarCategoria(Number(id), limpio);
      } else {
        await crearCategoria(limpio);
      }
      navigate("/categorias", {
        state: { aviso: editando ? "Categoría actualizada" : "Categoría creada" },
      });
    } catch (err) {
      if (err instanceof ErrorApi && err.errores.nombre) {
        setErrorNombre(err.errores.nombre);
      } else {
        setMensaje(
          err instanceof Error ? err.message : "No fue posible guardar la categoría.",
        );
      }
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="pagina-form form-categoria" onSubmit={guardar} noValidate>
      <div className="pagina">
        <h1>{titulo}</h1>

        {mensaje && <Aviso tipo="error" titulo={mensaje} />}

        {cargada && (
          <section className="tarjeta">
            <Campo
              etiqueta="Nombre"
              obligatorio
              value={nombre}
              maxLength={50}
              autoFocus
              error={errorNombre}
              onChange={cambiarNombre}
            />
          </section>
        )}
      </div>

      <div className="barra-acciones">
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate("/categorias")}
        >
          Cancelar
        </Button>
        <Button type="submit" variant="primary" disabled={!cargada || guardando}>
          Guardar
        </Button>
      </div>
    </form>
  );
}
