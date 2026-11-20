import { getApi, postApi } from "../../services/api";
import type { Captura, DetalleRevision, RevisionMecanico, RevisionTerminada, TipoRevision } from "./revisionesTypes";

// GET /api/movil/revisiones?tipo=SALIDA: mis revisiones por hacer (sin tipo, todas)
export const listarRevisiones = (tipo?: TipoRevision) =>
  getApi<RevisionMecanico[]>(tipo ? `/movil/revisiones?tipo=${tipo}` : "/movil/revisiones");

// GET /api/movil/revisiones/{id}: también sirve para el resumen del historial
export const obtenerRevision = (id: number) => getApi<DetalleRevision>(`/movil/revisiones/${id}`);

// POST /api/movil/revisiones/{id}/iniciar: al abrirla queda en proceso
export const iniciarRevision = (id: number) => postApi<DetalleRevision>(`/movil/revisiones/${id}/iniciar`, {});

// POST /api/movil/revisiones/{id}/terminar
export const terminarRevision = (id: number, captura: Captura) =>
  postApi<RevisionTerminada>(`/movil/revisiones/${id}/terminar`, captura);