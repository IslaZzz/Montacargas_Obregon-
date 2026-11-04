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
