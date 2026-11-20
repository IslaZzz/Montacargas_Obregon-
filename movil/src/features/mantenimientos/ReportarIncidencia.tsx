import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import { Dato, ListaDatos } from "../../components/Datos";
import Encabezado from "../../components/Encabezado";
import Pantalla from "../../components/Pantalla";
import Resultado from "../../components/Resultado";
import { Tarjeta } from "../../components/Tarjeta";
import CampoNota from "../../components/formulario/CampoNota";
import { mensajeDeError } from "../../services/api";
import { reportarIncidencia } from "./mantenimientosApi";
import { useMantenimiento } from "./useMantenimiento";

/** OHU19: no se pudo completar una tarea. Queda el aviso para el auxiliar y el mantenimiento sigue abierto. */
export default function ReportarIncidencia() {
  const navigate = useNavigate();
  const { id, mantenimiento, tarea, error } = useMantenimiento();
  const [mensaje, setMensaje] = useState("");
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const [enviado, setEnviado] = useState(false);

  const volver = `/mantenimientos/${id}`;

  if (enviado && mantenimiento) {
    return (
      <Resultado
        tipo="exito"
        icono="campaign"
        titulo="Aviso registrado"
        texto={`El mantenimiento de ${mantenimiento.unidad} sigue abierto hasta que se atienda la tarea.`}
        accion={{ texto: "Volver al mantenimiento", onClick: () => navigate(volver) }}
      />
    );
  }

  if (error || !mantenimiento || !tarea) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras={volver} titulo="Incidencia" />}>
        {error ? <Aviso tipo="error" titulo={error} /> : <Cargando />}
      </Pantalla>
    );
  }

  async function avisar() {
    if (!tarea) return;
    if (!mensaje.trim()) {
      setIntentado(true);
      return;
    }
    setEnviando(true);
    setFallo(null);
    try {
      await reportarIncidencia(id, tarea.id, mensaje.trim());
      setEnviado(true);
    } catch (e) {
      setFallo(mensajeDeError(e, "No se pudo registrar el aviso."));
      setEnviando(false);
    }
  }

  return (
    <Pantalla
      barraSuperior={
        <BarraSuperior atras={`${volver}/tareas/${tarea.id}`} titulo={`Equipo ${mantenimiento.unidad}`} chip={<Chip tono="azul">En curso</Chip>} />
      }
      barraAccion={
        <BarraAccion progreso={{ texto: "El mantenimiento sigue abierto", valor: mantenimiento.atendidas / Math.max(1, mantenimiento.tareas.length) }}>
          {fallo && <Aviso tipo="error" titulo={fallo} />}
          <Boton icono="campaign" cargando={enviando} onClick={avisar}>
            Avisar al auxiliar
          </Boton>
        </BarraAccion>
      }
    >
      <Encabezado titulo="Mantenimiento no completado" />

      <Tarjeta borde="error">
        <h3>Problema detectado</h3>
        <ListaDatos>
          <Dato etiqueta="Tarea">{tarea.titulo}</Dato>
          {tarea.hallazgo && (
            <Dato etiqueta="Hallazgo" apilado>
              {tarea.hallazgo}
            </Dato>
          )}
        </ListaDatos>
        <Chip tono="rojo" icono="error">
          Incidencia
        </Chip>
      </Tarjeta>

      <CampoNota
        etiqueta="Mensaje para el auxiliar"
        obligatorio
        placeholder="Ej. No hay refacción en almacén; se necesita pedirla"
        valor={mensaje}
        onCambio={setMensaje}
        maximo={500}
        error={intentado && !mensaje.trim() ? "Escribe el mensaje para el auxiliar" : undefined}
      />
    </Pantalla>
  );
}