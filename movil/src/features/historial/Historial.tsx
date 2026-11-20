import { useEffect, useState } from "react";
import Aviso from "../../components/Aviso";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import EstadoVacio from "../../components/EstadoVacio";
import PantallaPrincipal from "../../components/PantallaPrincipal";
import { LineaTarjeta, TarjetaEnlace, TituloTarjeta } from "../../components/Tarjeta";
import { mensajeDeError } from "../../services/api";
import { fechaHoraCorta } from "../../utils/fechas";
import { NOMBRE_TIPO_REVISION, TONO_TIPO_REVISION } from "../revisiones/revisionesTypes";
import { listarHistorial } from "./historialApi";
import type { Trabajo } from "./historialApi";

/** Las revisiones y mantenimientos que el mecánico terminó en los últimos 30 días. */
export default function Historial() {
  const [trabajos, setTrabajos] = useState<Trabajo[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listarHistorial()
      .then(setTrabajos)
      .catch((e) => setError(mensajeDeError(e, "No se pudo cargar tu historial.")));
  }, []);

  return (
    <PantallaPrincipal titulo="Historial">
      {error && <Aviso tipo="error" titulo={error} />}
      {!trabajos && !error && <Cargando />}
      {trabajos && trabajos.length === 0 && (
        <EstadoVacio>Aún no terminas revisiones ni mantenimientos en los últimos 30 días.</EstadoVacio>
      )}

      {trabajos?.map((t) => (
        <TarjetaEnlace
          key={`${t.trabajo}-${t.id}`}
          to={t.trabajo === "REVISION" ? `/historial/revision/${t.id}` : `/historial/mantenimiento/${t.id}`}
        >
          <TituloTarjeta>Equipo {t.unidad}</TituloTarjeta>
          <span className="chips">
            {t.trabajo === "REVISION" && t.tipoRevision ? (
              <Chip tono={TONO_TIPO_REVISION[t.tipoRevision]}>{NOMBRE_TIPO_REVISION[t.tipoRevision]}</Chip>
            ) : (
              <Chip tono="lila">Mantenimiento</Chip>
            )}
            {t.resultado === "REQUIERE_MANTENIMIENTO" && <Chip tono="rojo">Con falla</Chip>}
            {t.resultado === "CON_OBSERVACIONES" && <Chip tono="amarillo">Con observaciones</Chip>}
          </span>
          <LineaTarjeta icono="check_circle">Terminado · {fechaHoraCorta(t.terminado)}</LineaTarjeta>
          <LineaTarjeta icono="forklift">{t.tipoEquipo ?? "—"}</LineaTarjeta>
        </TarjetaEnlace>
      ))}
    </PantallaPrincipal>
  );
}