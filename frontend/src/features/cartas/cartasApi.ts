import { ErrorApi, getApi, postApi } from "../../services/apiClient";
import type { Carta, TipoFirma } from "./cartasTypes";

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

export interface DatosFirma {
  tipo: TipoFirma;
  fecha: string; // "2026-10-09T12:30"
  firmante: string;
  archivo: File;
}

// Registra la firma con la carta firmada (OHU-05). Va como formulario con archivo,
// por eso no usa postApi (que manda JSON). Si el backend la rechaza lanza ErrorApi con el motivo.
export async function registrarFirma(rentaId: number, datos: DatosFirma): Promise<Carta> {
  const formulario = new FormData();
  formulario.append("tipo", datos.tipo);
  formulario.append("fecha", datos.fecha);
  formulario.append("firmante", datos.firmante);
  formulario.append("archivo", datos.archivo);

  const respuesta = await fetch(`/api/rentas/${rentaId}/carta/firma`, { method: "POST", body: formulario });
  if (!respuesta.ok) {
    let mensaje = `No fue posible registrar la firma (error ${respuesta.status}).`;
    try {
      const cuerpo: { mensaje?: string } = await respuesta.json();
      if (cuerpo.mensaje) mensaje = cuerpo.mensaje;
    } catch {
      // la respuesta no trae JSON
    }
    throw new ErrorApi(mensaje);
  }
  return respuesta.json() as Promise<Carta>;
}

export function urlCartaFirmada(carta: Carta, descargar = false) {
  const url = `/api/rentas/${carta.rentaId}/carta/firmada`;
  return descargar ? `${url}?descargar=true` : url;
}

// La versión va en la dirección para que, al regenerarla, el navegador no muestre la anterior
export function urlPdfCarta(carta: Carta, descargar = false) {
  const url = `/api/rentas/${carta.rentaId}/carta/pdf?v=${carta.version ?? 0}`;
  return descargar ? `${url}&descargar=true` : url;
}
