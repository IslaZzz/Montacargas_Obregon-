import { deleteApi, getApi, postApi } from "./api";

// A qué pertenece una foto. Equivale a OrigenFoto del backend
export type OrigenFoto = "REVISION" | "TRASLADO" | "MANTENIMIENTO";

// Equivale a FotoResponse
export interface Foto {
  id: number;
  etiqueta: string; // "Frente", "Tablero y horómetro"…
  url: string; // Se usa directo en <img src>
  subidaEn: string;
}

// Lado más largo con que se guarda una foto; las del celular pesan varios MB
const LADO_MAXIMO = 1600;

/** Reduce la foto a JPEG antes de subirla. Si no se puede, sube la original. */
async function reducir(archivo: File): Promise<Blob> {
  try {
    const imagen = await createImageBitmap(archivo);
    const escala = Math.min(1, LADO_MAXIMO / Math.max(imagen.width, imagen.height));
    const lienzo = document.createElement("canvas");
    lienzo.width = Math.round(imagen.width * escala);
    lienzo.height = Math.round(imagen.height * escala);
    lienzo.getContext("2d")?.drawImage(imagen, 0, 0, lienzo.width, lienzo.height);
    const reducida = await new Promise<Blob | null>((listo) => lienzo.toBlob(listo, "image/jpeg", 0.8));
    return reducida ?? archivo;
  } catch {
    return archivo;
  }
}

/** POST /api/movil/fotos: sube una foto de una revisión, un traslado o un mantenimiento. */
export async function subirFoto(origen: OrigenFoto, referenciaId: number, etiqueta: string, archivo: File) {
  const datos = new FormData();
  datos.append("origen", origen);
  datos.append("referenciaId", String(referenciaId));
  datos.append("etiqueta", etiqueta);
  datos.append("archivo", await reducir(archivo), "foto.jpg");
  return postApi<Foto>("/movil/fotos", datos);
}

/** GET /api/movil/fotos: las fotos que ya se subieron de algo. */
export const listarFotos = (origen: OrigenFoto, referenciaId: number) =>
  getApi<Foto[]>(`/movil/fotos?origen=${origen}&referenciaId=${referenciaId}`);

/** DELETE /api/movil/fotos/{id}: para volver a tomarla. */
export const borrarFoto = (id: number) => deleteApi(`/movil/fotos/${id}`);