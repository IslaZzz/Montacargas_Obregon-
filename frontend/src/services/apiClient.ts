
// Función base para llamar a la API. <T> es el tipo de dato que se espera recibir.
export async function getApi<T>(
  ruta: string,
  opciones: RequestInit = {},
): Promise<T> {
  const respuesta = await fetch(`/api${ruta}`, {
    headers: { "Content-Type": "application/json" },
    ...opciones,
  });

  if (!respuesta.ok) throw new Error(`Error ${respuesta.status}`);

  return respuesta.json() as Promise<T>;
}

// Envía datos al backend y convierte sus errores en ErrorApi
async function enviarApi<T>(
  metodo: "POST" | "PATCH",
  ruta: string,
  cuerpo: unknown,
): Promise<T> {
  const respuesta = await fetch(`/api${ruta}`, {
    method: metodo,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(cuerpo),
  });

  if (!respuesta.ok) {
    let datos: { mensaje?: string; errores?: Record<string, string> } = {};
    try {
      datos = await respuesta.json();
    } catch {
      // la respuesta no trae JSON
    }
    throw new ErrorApi(
      datos.mensaje ?? `No fue posible guardar (error ${respuesta.status}).`,
      datos.errores,
    );
  }

  // 204: el backend respondió bien pero sin contenido
  if (respuesta.status === 204) return undefined as T;

  return respuesta.json() as Promise<T>;
}

// Crea un registro
export function postApi<T>(ruta: string, cuerpo: unknown): Promise<T> {
  return enviarApi<T>("POST", ruta, cuerpo);
}

// Modifica parte de un registro, por ejemplo asignar o cambiar un estado
export function patchApi<T>(ruta: string, cuerpo: unknown): Promise<T> {
  return enviarApi<T>("PATCH", ruta, cuerpo);
}

// Error que trae el mensaje y los errores por campo que envía el backend
export class ErrorApi extends Error {
  errores: Record<string, string>;

  constructor(mensaje: string, errores: Record<string, string> = {}) {
    super(mensaje);
    this.errores = errores;
  }
}