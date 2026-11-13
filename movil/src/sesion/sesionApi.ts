import { getApi, postApi } from "../services/api";
import type { Personal } from "./sesionTypes";

// POST /api/movil/sesion: busca a la persona por el código de su gafete
export const entrarConCodigo = (codigo: string) => postApi<Personal>("/movil/sesion", { codigo });

// GET /api/movil/yo: confirma que la persona guardada en el celular sigue activa
export const quienSoy = () => getApi<Personal>("/movil/yo");