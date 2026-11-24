import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import type { TonoChip } from "../../components/Chip";
import { Dato, ListaDatos } from "../../components/Datos";
import Domicilio from "../../components/Domicilio";
import Encabezado from "../../components/Encabezado";
import Icono from "../../components/Icono";
import Pantalla from "../../components/Pantalla";
import Resultado from "../../components/Resultado";
import { Tarjeta } from "../../components/Tarjeta";
import { conNotificacion } from "../../components/Notificacion";
import { mensajeDeError } from "../../services/api";
import { horario } from "../../utils/fechas";
import CartaResponsivaTarea from "./CartaResponsivaTarea";
import { completarTraslado, iniciarTraslado, obtenerTarea } from "./trasladosApi";
import type { DetalleTarea, EquipoTarea } from "./trasladosTypes";
import { NOMBRE_ESTADO_RENTA, NOMBRE_ESTADO_TRASLADO, NOMBRE_FASE } from "./trasladosTypes";

type Accion = "iniciar" | "completar";
type Hecho = { accion: Accion; titulo: string; texto: string };

/**
 * El detalle de una tarea del operador.
 * Entrega (OHU10 y OHU11): iniciar el traslado y registrar la entrega con la carta firmada.
 * Recolección (OHU13, OHU14 y OHU15): registrar la evidencia, confirmar la recolección
 * y confirmar la recepción en la oficina.
 */
export default function DetalleTraslado() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const [tarea, setTarea] = useState<DetalleTarea | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fallo, setFallo] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [hecho, setHecho] = useState<Hecho | null>(null);

  useEffect(() => {
    obtenerTarea(id)
      .then(setTarea)
      .catch((e) => setError(mensajeDeError(e, "No se pudo cargar la tarea.")));
  }, [id]);

  async function ejecutar(accion: Accion) {
    if (!tarea) return;
    setEnviando(true);
    setFallo(null);
    try {
      const actualizada = accion === "iniciar" ? await iniciarTraslado(id) : await completarTraslado(id);
      setTarea(actualizada);
      setHecho(mensajeDeHecho(accion, actualizada));
    } catch (e) {
      setFallo(mensajeDeError(e, "No se pudo registrar el movimiento."));
    } finally {
      setEnviando(false);
    }
  }

  const volver = () => navigate("/traslados", conNotificacion(avisoDeVuelta(hecho, tarea)));

  if (hecho && tarea) {
    const termino = hecho.accion === "completar";
    const etiqueta = tarea.tipo === "ENTREGA" ? "Entregado" : "Recibido";
    return (
      <Resultado
        tipo="exito"
        titulo={hecho.titulo}
        texto={hecho.texto}
        accion={{ texto: "Volver a mis tareas", onClick: volver }}
      >
        {termino && (
          <ul className="lista">
            {tarea.equipos.map((equipo) => (
              <li key={equipo.id}>
                <Tarjeta>
                  <div className="equipo">
                    <span className="equipo-unidad">{equipo.numeroUnidad}</span>
                    <Chip tono="verde">{etiqueta}</Chip>
                  </div>
                </Tarjeta>
              </li>
            ))}
          </ul>
        )}
      </Resultado>
    );
  }

  if (error) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras="/traslados" titulo="Tarea" />}>
        <Aviso tipo="error" titulo={error} />
      </Pantalla>
    );
  }

  if (!tarea) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras="/traslados" titulo="Tarea" />}>
        <Cargando />
      </Pantalla>
    );
  }

  const entrega = tarea.tipo === "ENTREGA";
  const enCurso = tarea.estado === "EN_CURSO";
  const programado = tarea.estado === "PROGRAMADO";
  const estado = estadoDeLaTarea(tarea);
  const tomadas = tarea.fotosTomadas ?? 0;
  const requeridas = tarea.fotosRequeridas ?? 0;

  return (
    <Pantalla
      barraSuperior={<BarraSuperior atras="/traslados" titulo={`Renta ${tarea.rentaFolio}`} />}
      barraAccion={
        tarea.estado === "COMPLETADO" ? undefined : (
          <BarraAccion progreso={progresoDe(tarea)}>
            {fallo && <Aviso tipo="error" titulo={fallo} />}
            <BotonPrincipal tarea={tarea} enviando={enviando} onAccion={ejecutar} />
          </BarraAccion>
        )
      }
    >
      <Encabezado titulo={tituloDe(tarea)}>
        <Icono nombre="schedule" />
        <span>{horario(tarea.inicio, tarea.fin)}</span>
      </Encabezado>

      <Tarjeta>
        <ListaDatos>
          <Dato etiqueta="Cliente">{tarea.cliente}</Dato>
          <Dato etiqueta={entrega ? "Entrega" : "Recolección"}>{tarea.folio}</Dato>
          <Dato etiqueta={entrega ? "Estado del traslado" : "Estado de la recolección"}>
            <Chip tono={estado.tono}>{estado.texto}</Chip>
          </Dato>
          <Dato etiqueta="Estado de la renta">
            <Chip tono={tarea.estadoRenta === "EN_CURSO" ? "azul" : "gris"}>
              {NOMBRE_ESTADO_RENTA[tarea.estadoRenta]}
            </Chip>
          </Dato>
        </ListaDatos>
      </Tarjeta>

      {!entrega && !programado ? (
        <Tarjeta>
          <h3>Regreso a la oficina</h3>
          <ListaDatos>
            <Dato etiqueta="Origen">
              <Domicilio domicilio={tarea.domicilio} />
            </Dato>
            <Dato etiqueta="Destino">Oficina de la empresa</Dato>
            <Dato etiqueta="Siguiente paso">Revisión mecánica</Dato>
          </ListaDatos>
        </Tarjeta>
      ) : (
        <Tarjeta>
          <h3>{entrega ? "Ubicación de entrega" : "Ubicación de recolección"}</h3>
          <Domicilio domicilio={tarea.domicilio} />
        </Tarjeta>
      )}

      {!entrega && (
        <Tarjeta borde={programado && tomadas < requeridas ? "atencion" : "normal"}>
          <h3>Evidencia fotográfica</h3>
          <ListaDatos>
            <Dato etiqueta="Progreso">
              {tomadas} de {requeridas} fotografías
            </Dato>
          </ListaDatos>
          <Link to={`/traslados/${tarea.id}/evidencia`} className="boton boton-secundario">
            <Icono nombre="photo_camera" />
            {programado ? "Registrar evidencia" : "Ver evidencia"}
          </Link>
        </Tarjeta>
      )}

      {tarea.bloqueo && (
        <Aviso tipo="error" titulo={entrega ? "Aún no puedes iniciar el traslado" : "Aún no puedes confirmar la recolección"}>
          {tarea.bloqueo}
        </Aviso>
      )}

      {!entrega && enCurso && (
        <Aviso tipo="info" titulo="Los equipos aún no han sido recibidos en la oficina." />
      )}

      {tarea.firmaEnSitio && !enCurso && (
        <Aviso tipo="advertencia" titulo="Lleva la carta responsiva">
          El cliente la firma al recibir los equipos.
        </Aviso>
      )}

      {entrega && enCurso && tarea.carta && <CartaResponsivaTarea tarea={tarea} onAdjuntada={setTarea} />}

      <h3 className="seccion-titulo">
        {tituloEquipos(tarea)} ({tarea.equipos.length})
      </h3>
      {tarea.equipos.map((equipo) => (
        <TarjetaEquipo key={equipo.id} equipo={equipo} entrega={entrega} />
      ))}
    </Pantalla>
  );
}

interface PropsBoton {
  tarea: DetalleTarea;
  enviando: boolean;
  onAccion: (accion: Accion) => void;
}

function BotonPrincipal({ tarea, enviando, onAccion }: PropsBoton) {
  const entrega = tarea.tipo === "ENTREGA";
  if (tarea.estado === "EN_CURSO") {
    return (
      <Boton icono="check" bloqueado={!tarea.puedeCompletar} cargando={enviando} onClick={() => onAccion("completar")}>
        {entrega ? "Marcar todos como entregados e instalados" : "Confirmar recepción en oficina"}
      </Boton>
    );
  }
  return (
    <Boton
      icono={entrega ? "local_shipping" : "inventory"}
      bloqueado={!tarea.puedeIniciar}
      cargando={enviando}
      onClick={() => onAccion("iniciar")}
    >
      {entrega ? "Iniciar traslado" : "Confirmar recolección"}
    </Boton>
  );
}

function TarjetaEquipo({ equipo, entrega }: { equipo: EquipoTarea; entrega: boolean }) {
  const recibido = equipo.fase === "EN_REVISION_ENTRADA";
  return (
    <Tarjeta borde={equipo.listo || recibido ? "normal" : "atencion"}>
      <div className="equipo">
        <span className="equipo-unidad">{equipo.numeroUnidad}</span>
        <Chip tono={tonoDeFase(equipo)}>{entrega ? NOMBRE_FASE[equipo.fase] : faseEnRecoleccion(equipo)}</Chip>
      </div>
      {!equipo.listo && !recibido && (
        <p className="tarjeta-linea">
          <Icono nombre="hourglass_top" />
          <span>{entrega ? "Aún no está listo para salir" : "Ya no está con el cliente"}</span>
        </p>
      )}
    </Tarjeta>
  );
}

function tituloDe(tarea: DetalleTarea) {
  if (tarea.tipo === "ENTREGA") {
    return tarea.estado === "EN_CURSO" ? "Entrega e instalación" : "Iniciar traslado";
  }
  if (tarea.estado === "EN_CURSO") return "Recibir equipo en oficina";
  if (tarea.estado === "COMPLETADO") return "Recolección recibida";
  return "Confirmar recolección";
}

function tituloEquipos(tarea: DetalleTarea) {
  if (tarea.tipo === "ENTREGA") return "Equipos de la renta";
  if (tarea.estado === "EN_CURSO") return "Equipos que regresan";
  if (tarea.estado === "COMPLETADO") return "Equipos recibidos";
  return "Equipos a recolectar";
}

// La recolección habla de "Pendiente" y "Recibida en oficina"; la entrega, del traslado
function estadoDeLaTarea(tarea: DetalleTarea): { texto: string; tono: TonoChip } {
  if (tarea.tipo === "ENTREGA") {
    return { texto: NOMBRE_ESTADO_TRASLADO[tarea.estado], tono: tarea.estado === "EN_CURSO" ? "lila" : "gris" };
  }
  if (tarea.estado === "EN_CURSO") return { texto: "En camino", tono: "lila" };
  if (tarea.estado === "COMPLETADO") return { texto: "Recibida en oficina", tono: "verde" };
  return { texto: "Pendiente", tono: "amarillo" };
}

function progresoDe(tarea: DetalleTarea) {
  const total = tarea.equipos.length;
  if (tarea.estado === "EN_CURSO") {
    return { texto: tarea.tipo === "ENTREGA" ? "Traslado iniciado" : "Recolección en curso", valor: 1, completo: true };
  }
  if (tarea.tipo === "RECOLECCION") {
    const tomadas = tarea.fotosTomadas ?? 0;
    const requeridas = tarea.fotosRequeridas ?? 0;
    return {
      texto: `${tomadas} de ${requeridas} fotografías`,
      valor: requeridas === 0 ? 0 : tomadas / requeridas,
      completo: requeridas > 0 && tomadas === requeridas,
    };
  }
  return {
    texto: `${tarea.listos} de ${total} equipos listos`,
    valor: total === 0 ? 0 : tarea.listos / total,
    completo: tarea.listos === total,
  };
}

function faseEnRecoleccion(equipo: EquipoTarea) {
  if (equipo.fase === "EN_OPERACION") return "Por recolectar";
  if (equipo.fase === "EN_TRASLADO") return "En traslado";
  if (equipo.fase === "EN_REVISION_ENTRADA") return "Recibido";
  return NOMBRE_FASE[equipo.fase];
}

function tonoDeFase(equipo: EquipoTarea): TonoChip {
  if (equipo.fase === "EN_TRASLADO") return "lila";
  if (equipo.fase === "EN_REVISION_ENTRADA") return "verde";
  return equipo.listo ? "verde" : "amarillo";
}

function mensajeDeHecho(accion: Accion, tarea: DetalleTarea): Hecho {
  const total = tarea.equipos.length;
  const equipos = `${total} equipo${total === 1 ? "" : "s"}`;
  const de = `${total} de ${total} equipo${total === 1 ? "" : "s"}`;
  const s = total === 1 ? "" : "s";

  if (tarea.tipo === "ENTREGA") {
    return accion === "iniciar"
      ? {
          accion,
          titulo: "Traslado iniciado",
          texto: `Renta ${tarea.rentaFolio} · los ${equipos} salieron de las instalaciones.`,
        }
      : {
          accion,
          titulo: "Entrega e instalación registradas",
          texto: `Renta ${tarea.rentaFolio} · ${de} entregado${s}.`,
        };
  }
  return accion === "iniciar"
    ? {
        accion,
        titulo: "Recolección registrada",
        texto: `Renta ${tarea.rentaFolio} · ${de} recolectado${s}.`,
      }
    : {
        accion,
        titulo: "Recepción registrada correctamente",
        texto: `Recibida en oficina · ${de} recibido${s}. El mecánico ya puede revisar los equipos.`,
      };
}

function avisoDeVuelta(hecho: Hecho | null, tarea: DetalleTarea | null) {
  if (!hecho || !tarea) return "";
  return `${tarea.folio}: ${hecho.titulo.toLowerCase()}`;
}