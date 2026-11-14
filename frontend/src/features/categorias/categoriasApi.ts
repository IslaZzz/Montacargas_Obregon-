import { getApi, postApi, putApi } from "../../services/apiClient";

export interface Categoria {
  id: number;
  nombre: string;
}

export const listarCategorias = () => getApi<Categoria[]>("/categorias");

export const obtenerCategoria = (id: number) =>
  getApi<Categoria>(`/categorias/${id}`);

export const crearCategoria = (nombre: string) =>
  postApi<Categoria>("/categorias", { nombre });

export const actualizarCategoria = (id: number, nombre: string) =>
  putApi<Categoria>(`/categorias/${id}`, { nombre });
