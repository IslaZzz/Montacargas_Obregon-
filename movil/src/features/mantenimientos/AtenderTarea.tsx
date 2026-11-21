import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import { Dato, ListaDatos } from "../../components/Datos";
import Encabezado from "../../components/Encabezado";
import Icono from "../../components/Icono";
import { conNotificacion } from "../../components/Notificacion";
import Pantalla from "../../components/Pantalla";
import { Tarjeta } from "../../components/Tarjeta";
import CampoFotos from "../../components/formulario/CampoFotos";
import CampoNota from "../../components/formulario/CampoNota";
import CampoTexto from "../../components/formulario/CampoTexto";
import { mensajeDeError } from "../../services/api";
import { listarFotos, subirFoto } from "../../services/fotosApi";
import type { Foto } from "../../services/fotosApi";
import { atenderTarea } from "./mantenimientosApi";
import { posicionDeTarea } from "./mantenimientosTypes";
import { useMantenimiento } from "./useMantenimiento";

/** OHU19: registrar lo que se hizo en una tarea. Si ya estaba atendida, se corrige. */
export default function AtenderTarea() {
  const navigate = useNavigate();
  const { id, mantenimiento, tarea, error } = useMantenimiento();

  const [trabajo, setTrabajo] = useState("");
  const [refacciones, setRefacciones] = useState("");
  const [tiempo, setTiempo] = useState("");
  const [observaciones, setObservaciones] = useState("");
  const [fotos, setFotos] = useState<Record<string, Foto>>({});
  const [intentado, setIntentado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  // Si se va a corregir, el formulario empieza con lo que ya se había guardado
  useEffect(() => {
    if (!tarea) return;
    setTrabajo(tarea.trabajoRealizado ?? "");
    setRefacciones(tarea.refacciones ?? "");
    setTiempo(tarea.tiempoInvertido ?? "");
    setObservaciones(tarea.observaciones ?? "");
    listarFotos("TAREA_MANTENIMIENTO", tarea.id)
      .then((subidas) => setFotos(Object.fromEntries(subidas.map((f) => [f.etiqueta, f]))))
      .catch(() => setFotos({}));
  }, [tarea]);

  const volver = `/mantenimientos/${id}`;

  if (error || !mantenimiento || !tarea) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras={volver} titulo="Atender tarea" />}>
        {error ? <Aviso tipo="error" titulo={error} /> : <Cargando />}
      </Pantalla>
    );
  }

  const editando = tarea.estado === "ATENDIDA";

  async function guardar() {
    if (!tarea) return;
    if (!trabajo.trim()) {
      setIntentado(true);
      return;
    }
    setEnviando(true);
    setFallo(null);
    try {
      await atenderTarea(id, tarea.id, {
        trabajoRealizado: trabajo.trim(),
        refacciones: refacciones.trim() || null,
        tiempoInvertido: tiempo.trim() || null,
        observaciones: observaciones.trim() || null,
      });
      navigate(volver, conNotificacion(editando ? "Cambios guardados" : `${tarea.titulo}: atendida`));
    } catch (e) {
      setFallo(mensajeDeError(e, "No se pudo guardar la atención."));
      setEnviando(false);
    }
  }

  return (
    <Pantalla
      barraSuperior={
        <BarraSuperior atras={volver} titulo={`Equipo ${mantenimiento.unidad}`} chip={<Chip tono="azul">En curso</Chip>} />
      }
      barraAccion={
        <BarraAccion
          progreso={{
            texto: posicionDeTarea(mantenimiento, tarea),
            valor: (mantenimiento.tareas.findIndex((t) => t.id === tarea.id) + 1) / mantenimiento.tareas.length,
          }}
        >
          {fallo && <Aviso tipo="error" titulo={fallo} />}
          <Boton icono="save" cargando={enviando} onClick={guardar}>
            Guardar atención
          </Boton>
        </BarraAccion>
      }
    >
      <Encabezado titulo={editando ? "Editar tarea" : "Atender tarea"}>
        <span>{tarea.titulo}</span>
      </Encabezado>

      <Tarjeta>
        <h3>{tarea.origen === "AGREGADA" ? "Agregada por ti" : "Hallazgo"}</h3>
        <ListaDatos>
          <Dato etiqueta="Qué se encontró" apilado>
            {tarea.hallazgo ?? tarea.titulo}
          </Dato>
          {tarea.componente && <Dato etiqueta="Componente">{tarea.componente}</Dato>}
          {tarea.accionSugerida && (
            <Dato etiqueta="Acción sugerida" apilado>
              {tarea.accionSugerida}
            </Dato>
          )}
        </ListaDatos>
      </Tarjeta>

      {tarea.incidencia && !editando && (
        <Aviso tipo="error" titulo="Tiene una incidencia">
          {tarea.incidencia}
        </Aviso>
      )}

      <CampoNota
        etiqueta="Trabajo realizado"
        obligatorio
        placeholder="Ej. Se cambió la manguera hidráulica"
        valor={trabajo}
        onCambio={setTrabajo}
        maximo={500}
        error={intentado && !trabajo.trim() ? "Describe el trabajo realizado" : undefined}
      />
      <CampoTexto
        etiqueta="Refacciones y materiales (opcional)"
        placeholder="Ej. Manguera 1/2 in, abrazaderas"
        maxLength={255}
        valor={refacciones}
        onCambio={setRefacciones}
      />
      <CampoTexto
        etiqueta="Tiempo invertido (opcional)"
        placeholder="Ej. 1 h 30 min"
        maxLength={50}
        valor={tiempo}
        onCambio={setTiempo}
      />
      <CampoFotos
        etiqueta="Fotos de esta tarea (opcional)"
        requeridas={mantenimiento.fotosPorTarea}
        subidas={fotos}
        onSubir={async (etiqueta, archivo) => {
          const foto = await subirFoto("TAREA_MANTENIMIENTO", tarea.id, etiqueta, archivo);
          setFotos((actual) => ({ ...actual, [etiqueta]: foto }));
        }}
      />
      <CampoNota etiqueta="Observaciones (opcional)" valor={observaciones} onCambio={setObservaciones} maximo={500} />

      {!editando && (
        <button type="button" className="boton-texto" onClick={() => navigate(`${volver}/tareas/${tarea.id}/incidencia`)}>
          <Icono nombre="report" />
          No puedo completar esta tarea
        </button>
      )}
    </Pantalla>
  );
}