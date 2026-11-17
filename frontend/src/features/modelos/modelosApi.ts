import { getApi, postApi, putApi } from "../../services/apiClient";
import type { CampoCategoria, Modelo, ModeloRequest } from "./modelosTypes";

export const listarModelos = () => getApi<Modelo[]>("/modelos");

export const obtenerModelo = (id: number) => getApi<Modelo>(`/modelos/${id}`);

export const crearModelo = (datos: ModeloRequest) => postApi<Modelo>("/modelos", datos);

export const actualizarModelo = (id: number, datos: ModeloRequest) =>
  putApi<Modelo>(`/modelos/${id}`, datos);

// Campos de especificación que pide la categoría (capacidad, altura del mástil...)
export const camposDeCategoria = (categoriaId: number) =>
  getApi<CampoCategoria[]>(`/categorias/${categoriaId}/campos`);
