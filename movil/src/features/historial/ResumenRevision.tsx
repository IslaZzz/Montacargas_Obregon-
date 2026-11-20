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
import { fechaHoraCorta } from "../../utils/fechas";
import { obtenerRevision } from "../revisiones/revisionesApi";
import type { DetalleRevision } from "../revisiones/revisionesTypes";
import { NOMBRE_TIPO_REVISION, TONO_TIPO_REVISION } from "../revisiones/revisionesTypes";
import ValoresRevision from "../revisiones/ValoresRevision";

/** Lo que se anotó en una revisión terminada. */
export default function ResumenRevision() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const [r, setR] = useState<DetalleRevision | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    obtenerRevision(id)
      .then(setR)
      .catch((e) => setError(mensajeDeError(e, "No se pudo cargar la revisión.")));
  }, [id]);

  if (error || !r) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras="/historial" titulo="Revisión" />}>
        {error ? <Aviso tipo="error" titulo={error} /> : <Cargando />}
      </Pantalla>
    );
  }

  const conFotos = r.valores.some((v) => v.tipo === "FOTO");

  return (
    <Pantalla
      barraSuperior={
        <BarraSuperior
          atras="/historial"
          titulo={`Equipo ${r.unidad}`}
          chip={<Chip tono={TONO_TIPO_REVISION[r.tipo]}>{NOMBRE_TIPO_REVISION[r.tipo]}</Chip>}
        />
      }
      barraAccion={
        <BarraAccion>
          <Boton icono="arrow_back" variante="secundario" onClick={() => navigate("/historial")}>
            Volver al historial
          </Boton>
        </BarraAccion>
      }
    >
      <Encabezado titulo={r.tipo === "SALIDA" ? "Revisión de salida" : "Revisión de regreso"} />

      <Tarjeta>
        <ListaDatos>
          <Dato etiqueta="Código">{r.unidad}</Dato>
          <Dato etiqueta="Tipo de equipo">{[r.tipoEquipo, r.modelo].filter(Boolean).join(" · ") || "—"}</Dato>
          <Dato etiqueta="Terminado">{r.fechaFin ? fechaHoraCorta(r.fechaFin) : "—"}</Dato>
        </ListaDatos>
      </Tarjeta>

      {r.valores.length === 0 ? (
        <Aviso tipo="info" titulo="Sin campos anotados">
          {r.notas ?? "Sin notas."}
        </Aviso>
      ) : (
        <Tarjeta>
          <h3>Lo que se revisó</h3>
          <ValoresRevision valores={r.valores} />
        </Tarjeta>
      )}

      {conFotos && (
        <Tarjeta>
          <h3>Fotos</h3>
          <GaleriaFotos origen="REVISION" referenciaId={r.id} />
        </Tarjeta>
      )}

      {r.valores.length > 0 && r.notas && (
        <Tarjeta>
          <h3>Notas</h3>
          <p>{r.notas}</p>
        </Tarjeta>
      )}
    </Pantalla>
  );
}