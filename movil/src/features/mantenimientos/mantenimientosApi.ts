import { getApi, postApi, putApi } from "../../services/api";
import type { Atencion, DetalleMantenimiento, MantenimientoMecanico } from "./mantenimientosTypes";

const base = (id: number) => `/movil/mantenimientos/${id}`;

// GET /api/movil/mantenimientos: mis mantenimientos nuevos y en curso
export const listarMantenimientos = () => getApi<MantenimientoMecanico[]>("/movil/mantenimientos");

// GET /api/movil/mantenimientos/{id}: también sirve para el resumen del historial
export const obtenerMantenimiento = (id: number) => getApi<DetalleMantenimiento>(base(id));

// POST .../iniciar: al abrirlo queda en curso
export const iniciarMantenimiento = (id: number) => postApi<DetalleMantenimiento>(`${base(id)}/iniciar`, {});

// PUT .../tareas/{tareaId}: atender o corregir una tarea
export const atenderTarea = (id: number, tareaId: number, atencion: Atencion) =>
  putApi<DetalleMantenimiento>(`${base(id)}/tareas/${tareaId}`, atencion);

// POST .../tareas: agregar algo que encontró y no venía en el mantenimiento
export const agregarTarea = (id: number, datos: { titulo: string; componente: string | null; accionSugerida: string | null }) =>
  postApi<DetalleMantenimiento>(`${base(id)}/tareas`, datos);

// POST .../tareas/{tareaId}/incidencia: no se pudo completar
export const reportarIncidencia = (id: number, tareaId: number, mensaje: string) =>
  postApi<DetalleMantenimiento>(`${base(id)}/tareas/${tareaId}/incidencia`, { mensaje });

// POST .../finalizar
export const finalizarMantenimiento = (id: number) => postApi<DetalleMantenimiento>(`${base(id)}/finalizar`, {});