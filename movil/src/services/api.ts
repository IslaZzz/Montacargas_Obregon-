import { leerPersonalId } from "../sesion/almacen";

// Encabezado con el que el backend sabe quién usa la app (lo lee PersonalActualResolver)
const ENCABEZADO_PERSONAL = "X-Personal-Id";

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

async function pedir<T>(metodo: string, ruta: string, cuerpo?: unknown): Promise<T> {
  const encabezados: Record<string, string> = {};
  const personalId = leerPersonalId();
  if (personalId !== null) encabezados[ENCABEZADO_PERSONAL] = String(personalId);

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
    throw new ErrorApi(
      datos.mensaje ?? `No fue posible completar la operación (error ${respuesta.status}).`,
      respuesta.status,
      datos.errores,
    );
  }

  // 204: respondió bien, sin contenido
  if (respuesta.status === 204) return undefined as T;
  return respuesta.json() as Promise<T>;
}

export const getApi = <T>(ruta: string) => pedir<T>("GET", ruta);
export const postApi = <T>(ruta: string, cuerpo: unknown) => pedir<T>("POST", ruta, cuerpo);
export const patchApi = <T>(ruta: string, cuerpo: unknown) => pedir<T>("PATCH", ruta, cuerpo);
export const deleteApi = (ruta: string) => pedir<void>("DELETE", ruta);