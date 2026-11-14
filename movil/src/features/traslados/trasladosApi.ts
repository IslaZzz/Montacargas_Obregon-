import { getApi, postApi } from "../../services/api";
import type { DetalleTarea, Tarea, TipoTraslado } from "./trasladosTypes";

// GET /api/movil/traslados?tipo=ENTREGA: mis entregas o mis recolecciones pendientes
export const listarTareas = (tipo: TipoTraslado) => getApi<Tarea[]>(`/movil/traslados?tipo=${tipo}`);

// GET /api/movil/traslados/{id}
export const obtenerTarea = (id: number) => getApi<DetalleTarea>(`/movil/traslados/${id}`);

// POST /api/movil/traslados/{id}/iniciar: salí con los equipos
export const iniciarTraslado = (id: number) => postApi<DetalleTarea>(`/movil/traslados/${id}/iniciar`, {});

// POST /api/movil/traslados/{id}/completar: ya entregué o ya recogí
export const completarTraslado = (id: number) => postApi<DetalleTarea>(`/movil/traslados/${id}/completar`, {});