import { useState } from "react";
import type { FormEvent } from "react";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Campo from "../../components/CampoForm";
import Modal from "../../components/Modal";
import { ErrorApi } from "../../services/apiClient";
import {
  actualizarCategoria,
  crearCategoria,
  type Categoria,
} from "./categoriasApi";

const MAXIMO = 50;

interface ModalCategoriaProps {
  categoria: Categoria | null;
  onCerrar: () => void;
  onGuardada: (categoria: Categoria) => void;
}

export default function ModalCategoria({
  categoria,
  onCerrar,
  onGuardada,
}: ModalCategoriaProps) {
  const [nombre, setNombre] = useState(categoria?.nombre ?? "");
  const [errorNombre, setErrorNombre] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [guardando, setGuardando] = useState(false);

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
      const guardada = categoria
        ? await actualizarCategoria(categoria.id, limpio)
        : await crearCategoria(limpio);
      onGuardada(guardada);
    } catch (err) {
      if (err instanceof ErrorApi && err.errores.nombre) {
        setErrorNombre(err.errores.nombre);
      } else {
        setMensaje(
          err instanceof Error ? err.message : "No fue posible guardar la categoría.",
        );
      }
      setGuardando(false);
    }
  }

  return (
    <Modal
      abierto
      titulo={categoria ? "Editar categoría" : "Nueva categoría"}
      onCerrar={onCerrar}
      acciones={
        <>
          <Button type="button" variant="secondary" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="primary"
            form="form-categoria"
            disabled={guardando}
          >
            Guardar
          </Button>
        </>
      }
    >
      <form id="form-categoria" onSubmit={guardar} noValidate>
        {mensaje && <Aviso tipo="error" titulo={mensaje} />}
        <Campo
          etiqueta="Nombre"
          obligatorio
          value={nombre}
          maxLength={MAXIMO}
          placeholder="Ej. Plataformas"
          autoFocus
          error={errorNombre}
          onChange={cambiarNombre}
        />
        <div className="categoria-ayuda">
          <span>Máximo {MAXIMO} caracteres</span>
          <span>
            {nombre.length} / {MAXIMO}
          </span>
        </div>
      </form>
    </Modal>
  );
}
