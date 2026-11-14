import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Aviso from "../../components/Aviso";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import EncabezadoGrupo from "../../components/EncabezadoGrupo";
import EstadoVacio from "../../components/EstadoVacio";
import Filtros from "../../components/Filtros";
import Icono from "../../components/Icono";
import PantallaPrincipal from "../../components/PantallaPrincipal";
import { mensajeDeError } from "../../services/api";
import { agruparPorDia, etiquetaDia, hora } from "../../utils/fechas";
import { lineasDomicilio } from "../../utils/domicilio";
import { listarTareas } from "./trasladosApi";
import type { Tarea, TipoTraslado } from "./trasladosTypes";

const FILTROS = [
  { valor: "ENTREGA" as TipoTraslado, etiqueta: "Entregas" },
  { valor: "RECOLECCION" as TipoTraslado, etiqueta: "Recolecciones" },
];

/** HU09: las entregas y recolecciones asignadas al operador, agrupadas por día. */
export default function MisTareas() {
  const [tipo, setTipo] = useState<TipoTraslado>("ENTREGA");
  const [tareas, setTareas] = useState<Tarea[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    setTareas(null);
    setError(null);

    listarTareas(tipo)
      .then((lista) => vigente && setTareas(lista))
      .catch((e) => vigente && setError(mensajeDeError(e, "No se pudieron cargar tus tareas.")));

    // Si se cambia de filtro antes de que llegue, se ignora la respuesta vieja
    return () => {
      vigente = false;
    };
  }, [tipo]);

  const grupos = tareas ? agruparPorDia(tareas, (t) => t.inicio) : [];

  return (
    <PantallaPrincipal titulo="Mis tareas">
      <Filtros etiqueta="Tipo de tarea" opciones={FILTROS} seleccionado={tipo} onCambio={setTipo} />

      {error && <Aviso tipo="error" titulo={error} />}
      {!tareas && !error && <Cargando />}

      {tareas && tareas.length === 0 && (
        <EstadoVacio>
          {tipo === "ENTREGA" ? "No tienes entregas asignadas." : "No tienes recolecciones asignadas."}
        </EstadoVacio>
      )}

      {grupos.map((grupo) => (
        <section key={grupo.dia} className="lista">
          <EncabezadoGrupo titulo={etiquetaDia(grupo.dia)} detalle={resumen(grupo.elementos)} />
          {grupo.elementos.map((tarea) => (
            <TarjetaTarea key={tarea.id} tarea={tarea} />
          ))}
        </section>
      ))}
    </PantallaPrincipal>
  );
}

/** "1 pendiente", "2 en camino", o las dos cosas. */
function resumen(tareas: Tarea[]) {
  const enCamino = tareas.filter((t) => t.estado === "EN_CURSO").length;
  const pendientes = tareas.length - enCamino;
  const partes: string[] = [];
  if (pendientes > 0) partes.push(`${pendientes} pendiente${pendientes === 1 ? "" : "s"}`);
  if (enCamino > 0) partes.push(`${enCamino} en camino`);
  return partes.join(" · ");
}

function TarjetaTarea({ tarea }: { tarea: Tarea }) {
  const enCamino = tarea.estado === "EN_CURSO";
  const entrega = tarea.tipo === "ENTREGA";

  return (
    <Link to={`/traslados/${tarea.id}`} className="tarjeta tarea">
      <span className="tarea-encabezado">
        {enCamino ? (
          <Chip tono="lila" icono="local_shipping">
            En camino
          </Chip>
        ) : (
          <Chip tono={entrega ? "verde" : "amarillo"} icono={entrega ? "north_east" : "south_west"}>
            {entrega ? "Entrega" : "Recolección"}
          </Chip>
        )}
        <span className="tarea-hora">{hora(tarea.inicio)}</span>
        <Icono nombre="chevron_right" className="tarea-flecha" />
      </span>

      <span className="tarjeta-titulo">{tarea.cliente}</span>
      <span className="tarea-folios">
        Renta {tarea.rentaFolio} · {tarea.folio}
      </span>

      <span className="tarjeta-linea">
        <Icono nombre="location_on" />
        <span>{lineasDomicilio(tarea.domicilio).join(", ")}</span>
      </span>
      <span className="tarjeta-linea">
        <Icono nombre="forklift" />
        <span>
          {tarea.equipos} equipo{tarea.equipos === 1 ? "" : "s"}
        </span>
      </span>

      {tarea.firmaEnSitio && (
        <Chip tono="naranja" icono="draw">
          Firma de carta en sitio
        </Chip>
      )}
    </Link>
  );
}