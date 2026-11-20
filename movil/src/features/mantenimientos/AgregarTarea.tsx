import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import Encabezado from "../../components/Encabezado";
import { conNotificacion } from "../../components/Notificacion";
import Pantalla from "../../components/Pantalla";
import CampoNota from "../../components/formulario/CampoNota";
import CampoTexto from "../../components/formulario/CampoTexto";
import { mensajeDeError } from "../../services/api";
import { agregarTarea } from "./mantenimientosApi";
import { useMantenimiento } from "./useMantenimiento";

/** OHU19: algo que el mecánico encontró y no venía en el mantenimiento. Queda como tarea pendiente. */
export default function AgregarTarea() {
  const navigate = useNavigate();
  const { id, mantenimiento, error } = useMantenimiento();
  const [titulo, setTitulo] = useState("");
  const [componente, setComponente] = useState("");
  const [accion, setAccion] = useState("");
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  const volver = `/mantenimientos/${id}`;

  if (error || !mantenimiento) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras={volver} titulo="Agregar tarea" />}>
        {error ? <Aviso tipo="error" titulo={error} /> : <Cargando />}
      </Pantalla>
    );
  }

  async function agregar() {
    if (!titulo.trim()) {
      setIntentado(true);
      return;
    }
    setEnviando(true);
    setFallo(null);
    try {
      await agregarTarea(id, {
        titulo: titulo.trim(),
        componente: componente.trim() || null,
        accionSugerida: accion.trim() || null,
      });
      navigate(volver, conNotificacion("Tarea agregada al mantenimiento"));
    } catch (e) {
      setFallo(mensajeDeError(e, "No se pudo agregar la tarea."));
      setEnviando(false);
    }
  }

  return (
    <Pantalla
      barraSuperior={
        <BarraSuperior atras={volver} titulo={`Equipo ${mantenimiento.unidad}`} chip={<Chip tono="azul">En curso</Chip>} />
      }
      barraAccion={
        <BarraAccion>
          {fallo && <Aviso tipo="error" titulo={fallo} />}
          <Boton icono="add" cargando={enviando} onClick={agregar}>
            Agregar tarea
          </Boton>
        </BarraAccion>
      }
    >
      <Encabezado titulo="Agregar tarea" />
      <Aviso tipo="info" titulo="Se agrega como tarea pendiente">
        La atiendes igual que las demás antes de finalizar el mantenimiento.
      </Aviso>

      <CampoTexto
        etiqueta="Qué encontraste"
        obligatorio
        placeholder="Ej. Fuga de aceite"
        maxLength={100}
        valor={titulo}
        onCambio={setTitulo}
        error={intentado && !titulo.trim() ? "Escribe qué encontraste" : undefined}
      />
      <CampoTexto
        etiqueta="Componente afectado (opcional)"
        placeholder="Ej. Cárter"
        maxLength={100}
        valor={componente}
        onCambio={setComponente}
      />
      <CampoNota
        etiqueta="Acción sugerida (opcional)"
        placeholder="Ej. Cambiar el empaque"
        valor={accion}
        onCambio={setAccion}
      />
    </Pantalla>
  );
}