import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { mensajeDeError } from "../../services/api";
import { obtenerMantenimiento } from "./mantenimientosApi";
import type { DetalleMantenimiento, Tarea } from "./mantenimientosTypes";

/** Carga el mantenimiento de la ruta (y la tarea, si la ruta trae una). Lo usan las pantallas hijas. */
export function useMantenimiento() {
  const params = useParams();
  const id = Number(params.id);
  const tareaId = params.tareaId ? Number(params.tareaId) : null;
  const [mantenimiento, setMantenimiento] = useState<DetalleMantenimiento | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    obtenerMantenimiento(id)
      .then(setMantenimiento)
      .catch((e) => setError(mensajeDeError(e, "No se pudo cargar el mantenimiento.")));
  }, [id]);

  const tarea: Tarea | null = mantenimiento?.tareas.find((t) => t.id === tareaId) ?? null;
  const noExiste = mantenimiento !== null && tareaId !== null && tarea === null;

  return { id, mantenimiento, tarea, error: error ?? (noExiste ? "La tarea no existe." : null) };
}