import { leerSesion } from "../sesion/almacen";

// Con el token que dio POST /api/movil/sesion el backend sabe quién usa la app (lo lee PersonalActualResolver)
const ENCABEZADO_AUTORIZACION = "Authorization";

// Error con el mensaje y los errores por campo que manda el backend: { mensaje, errores }
export class ErrorApi extends Error {
  estado: number;
  errores: Record<string, string>;

  constructor(mensaje: string, estado: number, errores: Record<string, string> = {}) {
    super(mensaje);
    this.estado = estado;
    this.errores = errores;
  }
}

// Mensaje para mostrar de cualquier error, sea del backend o de conexión
export function mensajeDeError(error: unknown, porDefecto = "No fue posible completar la operación.") {
  if (error instanceof ErrorApi) return error.message;
  return `${porDefecto} Revisa tu conexión.`;
}

// Quién atiende un 401 de una petición con sesión (la sesión venció o la cerraron): lo registra SesionProvider.
// Recibe el mensaje del backend y el token con el que se hizo la petición.
type AlSesionInvalida = (mensaje: string, tokenEnviado: string) => void;
let alSesionInvalida: AlSesionInvalida | null = null;

export function registrarAlSesionInvalida(atiende: AlSesionInvalida | null) {
  alSesionInvalida = atiende;
}

// Un código mal escrito también responde 401, pero eso es un error del formulario, no una sesión vencida
const esEntrar = (metodo: string, ruta: string) => metodo === "POST" && ruta === "/movil/sesion";

// comoArchivo: la respuesta es un archivo (la carta firmada), no JSON
async function pedir<T>(metodo: string, ruta: string, cuerpo?: unknown, comoArchivo = false): Promise<T> {
  const encabezados: Record<string, string> = {};
  const token = leerSesion()?.token ?? null;
  if (token !== null) encabezados[ENCABEZADO_AUTORIZACION] = `Bearer ${token}`;

  // FormData (fotos) lleva su propio Content-Type; lo demás va como JSON
  let body: BodyInit | undefined;
  if (cuerpo instanceof FormData) {
    body = cuerpo;
  } else if (cuerpo !== undefined) {
    encabezados["Content-Type"] = "application/json";
    body = JSON.stringify(cuerpo);
  }

  const respuesta = await fetch(`/api${ruta}`, { method: metodo, headers: encabezados, body });

  if (!respuesta.ok) {
    let datos: { mensaje?: string; errores?: Record<string, string> } = {};
    try {
      datos = await respuesta.json();
    } catch {
      // La respuesta no trae JSON
    }
    // 502, 503 o 504 sin mensaje del backend: el servidor no contestó (apagado o sin red)
    const sinServidor = [502, 503, 504].includes(respuesta.status);
    const mensaje =
      datos.mensaje ??
      (sinServidor
        ? "No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo."
        : `No fue posible completar la operación (error ${respuesta.status}).`);

    if (respuesta.status === 401 && token !== null && !esEntrar(metodo, ruta)) {
      alSesionInvalida?.(mensaje, token);
    }
    throw new ErrorApi(mensaje, respuesta.status, datos.errores);
  }

  // 204: respondió bien, sin contenido
  if (respuesta.status === 204) return undefined as T;
  if (comoArchivo) return respuesta.blob() as Promise<T>;
  return respuesta.json() as Promise<T>;
}

export const getApi = <T>(ruta: string) => pedir<T>("GET", ruta);
export const postApi = <T>(ruta: string, cuerpo: unknown) => pedir<T>("POST", ruta, cuerpo);
export const putApi = <T>(ruta: string, cuerpo: unknown) => pedir<T>("PUT", ruta, cuerpo);
export const patchApi = <T>(ruta: string, cuerpo: unknown) => pedir<T>("PATCH", ruta, cuerpo);
export const deleteApi = (ruta: string) => pedir<void>("DELETE", ruta);
export const getArchivoApi = (ruta: string) => pedir<Blob>("GET", ruta, undefined, true);