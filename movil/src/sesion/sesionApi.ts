import { deleteApi, getApi, postApi } from "../services/api";
import type { SesionIniciada, SesionVigente } from "./sesionTypes";

// POST /api/movil/sesion: entra con el código del gafete; devuelve el token y cuándo vence
export const entrarConCodigo = (codigo: string) => postApi<SesionIniciada>("/movil/sesion", { codigo });

// GET /api/movil/yo: confirma que la sesión guardada en el celular sigue vigente
export const quienSoy = () => getApi<SesionVigente>("/movil/yo");

// DELETE /api/movil/sesion: cierra la sesión en el servidor (204)
export const cerrarSesionEnServidor = () => deleteApi("/movil/sesion");
