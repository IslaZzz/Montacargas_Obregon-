import { getApi, getArchivoApi, postApi } from "../../services/api";
import { reducir } from "../../services/fotosApi";
import type { DetalleTarea, Evidencia, Tarea, TipoTraslado } from "./trasladosTypes";

// GET /api/movil/traslados?tipo=ENTREGA: mis entregas o mis recolecciones pendientes
export const listarTareas = (tipo: TipoTraslado) => getApi<Tarea[]>(`/movil/traslados?tipo=${tipo}`);

// GET /api/movil/traslados/{id}
export const obtenerTarea = (id: number) => getApi<DetalleTarea>(`/movil/traslados/${id}`);

// POST /api/movil/traslados/{id}/iniciar: salí con los equipos
export const iniciarTraslado = (id: number) => postApi<DetalleTarea>(`/movil/traslados/${id}/iniciar`, {});

// POST /api/movil/traslados/{id}/completar: ya entregué o ya recogí
export const completarTraslado = (id: number) => postApi<DetalleTarea>(`/movil/traslados/${id}/completar`, {});

// POST /api/movil/traslados/{id}/carta: el cliente firmó la carta impresa al recibir
export async function adjuntarCarta(id: number, firmante: string, foto: File) {
  const datos = new FormData();
  datos.append("firmante", firmante);
  datos.append("archivo", await reducir(foto), "carta.jpg");
  return postApi<DetalleTarea>(`/movil/traslados/${id}/carta`, datos);
}

// GET /api/movil/traslados/{id}/carta: la carta firmada (PDF o foto)
export const descargarCarta = (id: number) => getArchivoApi(`/movil/traslados/${id}/carta`);

// GET /api/movil/traslados/{id}/evidencia: las fotos de la recolección por equipo
export const obtenerEvidencia = (id: number) => getApi<Evidencia>(`/movil/traslados/${id}/evidencia`);

// POST /api/movil/traslados/{id}/evidencia: sube o cambia una vista de un equipo
export async function subirEvidencia(id: number, equipoId: number, vista: string, foto: File) {
  const datos = new FormData();
  datos.append("equipoId", String(equipoId));
  datos.append("vista", vista);
  datos.append("archivo", await reducir(foto), "evidencia.jpg");
  return postApi<Evidencia>(`/movil/traslados/${id}/evidencia`, datos);
}