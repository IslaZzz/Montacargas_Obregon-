
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

export async function postApi<T>(ruta: string, cuerpo: unknown): Promise<T> {
  const respuesta = await fetch(`/api${ruta}`, {
    method: "POST",
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
  return respuesta.json() as Promise<T>;
}

// Error que trae el mensaje y los errores por campo que envía el backend
export class ErrorApi extends Error {
  errores: Record<string, string>;

  constructor(mensaje: string, errores: Record<string, string> = {}) {
    super(mensaje);
    this.errores = errores;
  }
}
