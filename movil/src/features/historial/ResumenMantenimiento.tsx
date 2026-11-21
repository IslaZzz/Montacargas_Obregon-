import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import { Dato, ListaDatos } from "../../components/Datos";
import Encabezado from "../../components/Encabezado";
import GaleriaFotos from "../../components/GaleriaFotos";
import Pantalla from "../../components/Pantalla";
import { Tarjeta } from "../../components/Tarjeta";
import { mensajeDeError } from "../../services/api";
import ValoresRevision from "../revisiones/ValoresRevision";
import DatosEquipo from "../mantenimientos/DatosEquipo";
import { obtenerMantenimiento } from "../mantenimientos/mantenimientosApi";
import type { DetalleMantenimiento } from "../mantenimientos/mantenimientosTypes";
import { estadoDeTarea } from "../mantenimientos/mantenimientosTypes";

/** Lo que se registró en un mantenimiento terminado, tarea por tarea. */
export default function ResumenMantenimiento() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const [m, setM] = useState<DetalleMantenimiento | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    obtenerMantenimiento(id)
      .then(setM)
      .catch((e) => setError(mensajeDeError(e, "No se pudo cargar el mantenimiento.")));
  }, [id]);

  if (error || !m) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras="/historial" titulo="Mantenimiento" />}>
        {error ? <Aviso tipo="error" titulo={error} /> : <Cargando />}
      </Pantalla>
    );
  }

  return (
    <Pantalla
      barraSuperior={<BarraSuperior atras="/historial" titulo={`Equipo ${m.unidad}`} chip={<Chip tono="verde">Terminado</Chip>} />}
      barraAccion={
        <BarraAccion progreso={{ texto: `Cerrado · ${m.atendidas} tarea${m.atendidas === 1 ? "" : "s"} atendida${m.atendidas === 1 ? "" : "s"}`, valor: 1, completo: true }}>
          <Boton icono="arrow_back" variante="secundario" onClick={() => navigate("/historial")}>
            Volver al historial
          </Boton>
        </BarraAccion>
      }
    >
      <Encabezado titulo="Resumen del mantenimiento" />
      <DatosEquipo mantenimiento={m} />

      {m.revision.length > 0 && (
        <Tarjeta>
          <h3>Lo que se anotó en la revisión</h3>
          <ValoresRevision valores={m.revision} />
        </Tarjeta>
      )}

      {m.tareas.length === 0 && (
        <Aviso tipo="info" titulo="Se cerró desde la oficina">
          {m.descripcionFalla}
        </Aviso>
      )}

      {m.tareas.map((t) => (
        <Tarjeta key={t.id}>
          <h3>{t.titulo}</h3>
          <span className="chips">
            <Chip tono={estadoDeTarea(t).tono}>{estadoDeTarea(t).texto}</Chip>
            {t.origen === "AGREGADA" && <Chip tono="gris">Agregada</Chip>}
          </span>
          <ListaDatos>
            {t.hallazgo && (
              <Dato etiqueta="Hallazgo" apilado>
                {t.hallazgo}
              </Dato>
            )}
            {t.trabajoRealizado && (
              <Dato etiqueta="Trabajo realizado" apilado>
                {t.trabajoRealizado}
              </Dato>
            )}
            {t.refacciones && (
              <Dato etiqueta="Refacciones y materiales" apilado>
                {t.refacciones}
              </Dato>
            )}
            {t.tiempoInvertido && <Dato etiqueta="Tiempo invertido">{t.tiempoInvertido}</Dato>}
            {t.observaciones && (
              <Dato etiqueta="Observaciones" apilado>
                {t.observaciones}
              </Dato>
            )}
          </ListaDatos>
          <GaleriaFotos origen="TAREA_MANTENIMIENTO" referenciaId={t.id} />
        </Tarjeta>
      ))}
    </Pantalla>
  );
}