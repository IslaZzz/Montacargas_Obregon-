import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
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
import { completarTraslado, iniciarTraslado, obtenerTarea } from "./trasladosApi";
import type { DetalleTarea, EquipoTarea } from "./trasladosTypes";
import { NOMBRE_ESTADO_RENTA, NOMBRE_FASE } from "./trasladosTypes";

type Hecho = { titulo: string; texto: string };

/** OHU10: el detalle de la tarea, con iniciar el traslado y marcarlo como realizado. */
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

  async function ejecutar(accion: "iniciar" | "completar") {
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

  if (hecho) {
    return (
      <Resultado
        tipo="exito"
        titulo={hecho.titulo}
        texto={hecho.texto}
        accion={{ texto: "Volver a mis tareas", onClick: volver }}
      />
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

  return (
    <Pantalla
      barraSuperior={<BarraSuperior atras="/traslados" titulo={`Renta ${tarea.rentaFolio}`} />}
      barraAccion={
        <BarraAccion
          progreso={{
            texto: enCurso
              ? "Traslado iniciado"
              : `${tarea.listos} de ${tarea.equipos.length} equipos listos`,
            valor: tarea.equipos.length === 0 ? 0 : tarea.listos / tarea.equipos.length,
            completo: enCurso || tarea.listos === tarea.equipos.length,
          }}
        >
          {fallo && <Aviso tipo="error" titulo={fallo} />}
          {enCurso ? (
            <Boton icono="check" cargando={enviando} onClick={() => ejecutar("completar")}>
              {entrega ? "Marcar entrega realizada" : "Marcar recolección realizada"}
            </Boton>
          ) : (
            <Boton
              icono="local_shipping"
              bloqueado={!tarea.puedeIniciar}
              cargando={enviando}
              onClick={() => ejecutar("iniciar")}
            >
              Iniciar traslado
            </Boton>
          )}
        </BarraAccion>
      }
    >
      <Encabezado titulo={enCurso ? "Traslado en curso" : "Iniciar traslado"}>
        <Icono nombre="schedule" />
        <span>{horario(tarea.inicio, tarea.fin)}</span>
      </Encabezado>

      <Tarjeta>
        <ListaDatos>
          <Dato etiqueta="Cliente">{tarea.cliente}</Dato>
          <Dato etiqueta={entrega ? "Entrega" : "Recolección"}>{tarea.folio}</Dato>
          <Dato etiqueta="Estado de la renta">
            <Chip tono={tarea.estadoRenta === "EN_CURSO" ? "azul" : "gris"}>
              {NOMBRE_ESTADO_RENTA[tarea.estadoRenta]}
            </Chip>
          </Dato>
        </ListaDatos>
      </Tarjeta>

      <Tarjeta>
        <h3>{entrega ? "Ubicación de entrega" : "Origen de la recolección"}</h3>
        <Domicilio domicilio={tarea.domicilio} mapa />
      </Tarjeta>

      {tarea.bloqueo && (
        <Aviso tipo="error" titulo="Aún no puedes iniciar el traslado">
          {tarea.bloqueo}
        </Aviso>
      )}

      {tarea.firmaEnSitio && (
        <Aviso tipo="advertencia" titulo="Lleva la carta responsiva">
          El cliente la firma al recibir los equipos.
        </Aviso>
      )}

      <h3 className="seccion-titulo">
        {entrega ? "Equipos de la renta" : "Equipos a recolectar"} ({tarea.equipos.length})
      </h3>
      {tarea.equipos.map((equipo) => (
        <TarjetaEquipo key={equipo.id} equipo={equipo} />
      ))}
    </Pantalla>
  );
}

function TarjetaEquipo({ equipo }: { equipo: EquipoTarea }) {
  return (
    <Tarjeta borde={equipo.listo ? "normal" : "atencion"}>
      <div className="equipo">
        <span className="equipo-unidad">{equipo.numeroUnidad}</span>
        <Chip tono={tonoDeFase(equipo)}>{NOMBRE_FASE[equipo.fase]}</Chip>
      </div>
      {!equipo.listo && (
        <p className="tarjeta-linea">
          <Icono nombre="hourglass_top" />
          <span>Aún no está listo para salir</span>
        </p>
      )}
    </Tarjeta>
  );
}

function tonoDeFase(equipo: EquipoTarea) {
  if (equipo.fase === "EN_TRASLADO") return "lila" as const;
  return equipo.listo ? ("verde" as const) : ("amarillo" as const);
}

function mensajeDeHecho(accion: "iniciar" | "completar", tarea: DetalleTarea): Hecho {
  const equipos = `${tarea.equipos.length} equipo${tarea.equipos.length === 1 ? "" : "s"}`;
  if (accion === "iniciar") {
    return {
      titulo: "Traslado iniciado",
      texto:
        tarea.tipo === "ENTREGA"
          ? `Renta ${tarea.rentaFolio} · los ${equipos} salieron de las instalaciones.`
          : `Renta ${tarea.rentaFolio} · vas por los ${equipos}.`,
    };
  }
  return tarea.tipo === "ENTREGA"
    ? { titulo: "Entrega realizada", texto: `Renta ${tarea.rentaFolio} · los ${equipos} quedaron con el cliente.` }
    : { titulo: "Recolección realizada", texto: `Renta ${tarea.rentaFolio} · los ${equipos} regresan a revisión.` };
}

function avisoDeVuelta(hecho: Hecho | null, tarea: DetalleTarea | null) {
  if (!hecho || !tarea) return "";
  return `${tarea.folio}: ${hecho.titulo.toLowerCase()}`;
}