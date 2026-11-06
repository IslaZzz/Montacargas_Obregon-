import { getApi, postApi } from "../../services/apiClient";
import type { Carta } from "./cartasTypes";

export const obtenerCarta = (rentaId: number | string) =>
  getApi<Carta>(`/rentas/${rentaId}/carta`);

// Genera la carta o, si ya existía, la regenera. Si faltan datos lanza ErrorApi con
// los datos que faltan en "errores" (respuesta 422).
export const generarCarta = (rentaId: number) =>
  postApi<Carta>(`/rentas/${rentaId}/carta`, {});

// Envía la carta al correo del cliente. Si el correo falla lanza ErrorApi (502 o 503)
// y la carta se queda como estaba.
export const enviarCarta = (rentaId: number, mensaje: string) =>
  postApi<Carta>(`/rentas/${rentaId}/carta/envio`, { mensaje });

// La versión va en la dirección para que, al regenerarla, el navegador no muestre la anterior
export function urlPdfCarta(carta: Carta, descargar = false) {
  const url = `/api/rentas/${carta.rentaId}/carta/pdf?v=${carta.version ?? 0}`;
  return descargar ? `${url}&descargar=true` : url;
}
