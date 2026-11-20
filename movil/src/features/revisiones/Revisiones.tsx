import { useEffect, useState } from "react";
import Aviso from "../../components/Aviso";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import EstadoVacio from "../../components/EstadoVacio";
import Filtros from "../../components/Filtros";
import PantallaPrincipal from "../../components/PantallaPrincipal";
import { LineaTarjeta, TarjetaEnlace, TituloTarjeta } from "../../components/Tarjeta";
import { mensajeDeError } from "../../services/api";
import { fechaHoraCorta } from "../../utils/fechas";
import { listarRevisiones } from "./revisionesApi";
import type { RevisionMecanico, TipoRevision } from "./revisionesTypes";
import { NOMBRE_TIPO_REVISION, TONO_TIPO_REVISION } from "./revisionesTypes";

type Filtro = "TODAS" | TipoRevision;

const FILTROS: { valor: Filtro; etiqueta: string }[] = [
  { valor: "TODAS", etiqueta: "Todos" },
  { valor: "SALIDA", etiqueta: "Salida" },
  { valor: "ENTRADA", etiqueta: "Regreso" },
];

const VACIO: Record<Filtro, string> = {
  TODAS: "No tienes revisiones pendientes.",
  SALIDA: "Sin salidas pendientes.",
  ENTRADA: "Sin regresos pendientes.",
};

/** OHU06: los equipos que el mecánico tiene por revisar, de salida o de regreso. */
export default function Revisiones() {
  const [filtro, setFiltro] = useState<Filtro>("TODAS");
  const [revisiones, setRevisiones] = useState<RevisionMecanico[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listarRevisiones()
      .then(setRevisiones)
      .catch((e) => setError(mensajeDeError(e, "No se pudieron cargar tus revisiones.")));
  }, []);

  // Se filtra aquí: la lista es corta y así cambiar de filtro es inmediato
  const visibles = revisiones?.filter((r) => filtro === "TODAS" || r.tipo === filtro) ?? [];

  return (
    <PantallaPrincipal titulo="Equipos por revisar">
      <Filtros etiqueta="Tipo de revisión" opciones={FILTROS} seleccionado={filtro} onCambio={setFiltro} />

      {error && <Aviso tipo="error" titulo={error} />}
      {!revisiones && !error && <Cargando />}
      {revisiones && visibles.length === 0 && <EstadoVacio>{VACIO[filtro]}</EstadoVacio>}

      {visibles.map((r) => (
        <TarjetaEnlace key={r.id} to={`/revisiones/${r.id}`}>
          <TituloTarjeta>{r.unidad}</TituloTarjeta>
          <span className="chips">
            <Chip tono={TONO_TIPO_REVISION[r.tipo]}>{NOMBRE_TIPO_REVISION[r.tipo]}</Chip>
            {r.estado === "EN_PROCESO" && <Chip tono="azul">En progreso</Chip>}
          </span>
          <LineaTarjeta icono="schedule">{fechaHoraCorta(r.fechaProgramada)}</LineaTarjeta>
          <LineaTarjeta icono="forklift">
            {[r.tipoEquipo, `Renta ${r.rentaFolio}`].filter(Boolean).join(" · ")}
          </LineaTarjeta>
        </TarjetaEnlace>
      ))}
    </PantallaPrincipal>
  );
}