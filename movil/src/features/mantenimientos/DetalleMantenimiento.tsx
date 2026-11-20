import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import Encabezado from "../../components/Encabezado";
import { conNotificacion } from "../../components/Notificacion";
import Pantalla from "../../components/Pantalla";
import Resultado from "../../components/Resultado";
import { LineaTarjeta, Tarjeta, TarjetaEnlace, TituloTarjeta } from "../../components/Tarjeta";
import { mensajeDeError } from "../../services/api";
import ValoresRevision from "../revisiones/ValoresRevision";
import DatosEquipo from "./DatosEquipo";
import { finalizarMantenimiento, iniciarMantenimiento } from "./mantenimientosApi";
import type { DetalleMantenimiento as Detalle } from "./mantenimientosTypes";
import { estadoDeTarea, NOMBRE_ESTADO, TONO_ESTADO } from "./mantenimientosTypes";

/** OHU19: el mantenimiento con sus tareas; se atiende cada una y al final se finaliza. */
export default function DetalleMantenimiento() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const [m, setM] = useState<Detalle | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fallo, setFallo] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [finalizado, setFinalizado] = useState(false);

  // Al abrirlo queda en curso (y, si venía de la oficina, su falla se vuelve la primera tarea)
  useEffect(() => {
    iniciarMantenimiento(id)
      .then(setM)
      .catch((e) => setError(mensajeDeError(e, "No se pudo abrir el mantenimiento.")));
  }, [id]);

  async function finalizar() {
    setEnviando(true);
    setFallo(null);
    try {
      setM(await finalizarMantenimiento(id));
      setFinalizado(true);
    } catch (e) {
      setFallo(mensajeDeError(e, "No se pudo finalizar el mantenimiento."));
    } finally {
      setEnviando(false);
    }
  }

  if (finalizado && m) {
    return (
      <Resultado
        tipo="exito"
        titulo="Mantenimiento finalizado"
        texto={`${m.unidad} quedó operativo. El auxiliar ya lo ve en la oficina.`}
        accion={{
          texto: "Volver a mantenimientos",
          onClick: () => navigate("/mantenimientos", conNotificacion(`${m.unidad} pasó al historial`)),
        }}
      />
    );
  }

  if (error || !m) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras="/mantenimientos" titulo="Mantenimiento" />}>
        {error ? <Aviso tipo="error" titulo={error} /> : <Cargando />}
      </Pantalla>
    );
  }

  const pendientes = m.tareas.length - m.atendidas;

  return (
    <Pantalla
      barraSuperior={
        <BarraSuperior
          atras="/mantenimientos"
          titulo={`Equipo ${m.unidad}`}
          chip={<Chip tono={TONO_ESTADO[m.estado] ?? "gris"}>{NOMBRE_ESTADO[m.estado] ?? m.estado}</Chip>}
        />
      }
      barraAccion={
        <BarraAccion
          progreso={{
            texto:
              pendientes === 0
                ? `${m.atendidas} de ${m.tareas.length} tareas atendidas`
                : `${m.atendidas} atendida${m.atendidas === 1 ? "" : "s"} · ${pendientes} pendiente${pendientes === 1 ? "" : "s"}`,
            valor: m.tareas.length === 0 ? 0 : m.atendidas / m.tareas.length,
            completo: m.puedeFinalizar,
          }}
        >
          {fallo && <Aviso tipo="error" titulo={fallo} />}
          <Boton icono="task_alt" bloqueado={!m.puedeFinalizar} cargando={enviando} onClick={finalizar}>
            Finalizar mantenimiento
          </Boton>
        </BarraAccion>
      }
    >
      <Encabezado titulo="Mantenimiento">
        <span>{m.tipo === "PREVENTIVO" ? "Preventivo" : "Correctivo"}</span>
      </Encabezado>

      <DatosEquipo mantenimiento={m} />

      {m.revision.length > 0 && (
        <Tarjeta>
          <h3>Lo que se anotó en la revisión</h3>
          <ValoresRevision valores={m.revision} />
        </Tarjeta>
      )}

      <h3 className="seccion-titulo">Trabajo asignado</h3>
      {m.tareas.map((tarea) => {
        const estado = estadoDeTarea(tarea);
        return (
          <TarjetaEnlace
            key={tarea.id}
            to={`/mantenimientos/${m.id}/tareas/${tarea.id}`}
            borde={tarea.incidencia && tarea.estado !== "ATENDIDA" ? "error" : "normal"}
          >
            <TituloTarjeta>{tarea.titulo}</TituloTarjeta>
            <span className="chips">
              <Chip tono={estado.tono}>{estado.texto}</Chip>
              {tarea.origen === "AGREGADA" && <Chip tono="gris">Agregada</Chip>}
            </span>
            {tarea.hallazgo && <LineaTarjeta icono="search">{tarea.hallazgo}</LineaTarjeta>}
            {tarea.estado === "ATENDIDA" && tarea.trabajoRealizado && (
              <LineaTarjeta icono="build">{tarea.trabajoRealizado}</LineaTarjeta>
            )}
          </TarjetaEnlace>
        );
      })}

      <Boton variante="secundario" icono="add" onClick={() => navigate(`/mantenimientos/${m.id}/agregar`)}>
        Agregar tarea
      </Boton>
    </Pantalla>
  );
}