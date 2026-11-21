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
import { listarMantenimientos } from "./mantenimientosApi";
import type { EstadoMantenimiento, MantenimientoMecanico } from "./mantenimientosTypes";
import { NOMBRE_ESTADO, TONO_ESTADO } from "./mantenimientosTypes";

type Filtro = "TODOS" | Extract<EstadoMantenimiento, "EN_PROCESO" | "ASIGNADO">;

const FILTROS: { valor: Filtro; etiqueta: string }[] = [
  { valor: "TODOS", etiqueta: "Todos" },
  { valor: "EN_PROCESO", etiqueta: "En curso" },
  { valor: "ASIGNADO", etiqueta: "Nuevos" },
];

const VACIO: Record<Filtro, string> = {
  TODOS: "No tienes mantenimientos asignados.",
  EN_PROCESO: "Sin mantenimientos en curso.",
  ASIGNADO: "Sin mantenimientos nuevos.",
};

/** OHU18: los mantenimientos asignados al mecánico. */
export default function Mantenimientos() {
  const [filtro, setFiltro] = useState<Filtro>("TODOS");
  const [lista, setLista] = useState<MantenimientoMecanico[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listarMantenimientos()
      .then(setLista)
      .catch((e) => setError(mensajeDeError(e, "No se pudieron cargar tus mantenimientos.")));
  }, []);

  const visibles = lista?.filter((m) => filtro === "TODOS" || m.estado === filtro) ?? [];

  return (
    <PantallaPrincipal titulo="Mantenimientos">
      <Filtros etiqueta="Estado del mantenimiento" opciones={FILTROS} seleccionado={filtro} onCambio={setFiltro} />

      {error && <Aviso tipo="error" titulo={error} />}
      {!lista && !error && <Cargando />}
      {lista && visibles.length === 0 && <EstadoVacio>{VACIO[filtro]}</EstadoVacio>}

      {visibles.map((m) => (
        <TarjetaEnlace key={m.id} to={`/mantenimientos/${m.id}`}>
          <TituloTarjeta>Equipo {m.unidad}</TituloTarjeta>
          <Chip tono={TONO_ESTADO[m.estado] ?? "gris"}>{NOMBRE_ESTADO[m.estado] ?? m.estado}</Chip>
          <LineaTarjeta icono="schedule">
            {m.fechaLimite ? `Fecha límite · ${fechaHoraCorta(m.fechaLimite)}` : "Sin fecha límite"}
          </LineaTarjeta>
          <LineaTarjeta icono="build">
            {[m.tipoEquipo, m.tipo === "PREVENTIVO" ? "Preventivo" : "Correctivo"].filter(Boolean).join(" · ")}
          </LineaTarjeta>
        </TarjetaEnlace>
      ))}
    </PantallaPrincipal>
  );
}