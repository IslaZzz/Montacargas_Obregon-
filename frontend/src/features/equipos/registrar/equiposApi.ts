import { getApi, postApi } from "../../../services/apiClient";

// Equivale a EtiquetaResponse
export interface Etiqueta {
  id: number;
  nombre: string; // Ej. "Uso en interiores"
}

// Equivale a EquipoRequest
export interface EquipoRequest {
  modeloId: number;
  numeroUnidad: string;
  numeroSerie: string;
  anioCompra: number | null;
  etiquetaIds: number[];
}

export const listarEtiquetas = () => getApi<Etiqueta[]>("/etiquetas");

export const crearEtiqueta = (nombre: string) => postApi<Etiqueta>("/etiquetas", { nombre });

// El equipo queda «En revisión de entrada» hasta que Operativo lo pone disponible
export const registrarEquipo = (datos: EquipoRequest) => postApi<{ id: number }>("/equipos", datos);
