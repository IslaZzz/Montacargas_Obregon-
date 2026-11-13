// Domicilio como lo manda el backend (DomicilioResponse).
// Hoy la renta guarda el domicilio en un solo texto; cuando se capture por código postal
// llegarán también las partes. Las pantallas solo usan estas funciones, así que no cambian.
export interface Domicilio {
  texto: string; // Siempre viene: el domicilio completo en una línea
  calle: string | null;
  numero: string | null;
  colonia: string | null;
  codigoPostal: string | null;
  ciudad: string | null;
  estado: string | null;
}

const tienePartes = (d: Domicilio) => Boolean(d.calle && d.colonia && d.codigoPostal);

/**
 * Las líneas que se muestran: con partes, "Calle Náinari 450" y "Col. Centro, 85000 Cd. Obregón, Sonora";
 * sin partes, el texto tal cual.
 */
export function lineasDomicilio(d: Domicilio): string[] {
  if (!tienePartes(d)) return [d.texto];
  const lugar = [d.ciudad, d.estado].filter(Boolean).join(", ");
  return [
    [d.calle, d.numero].filter(Boolean).join(" "),
    `Col. ${d.colonia}, ${d.codigoPostal}${lugar ? ` ${lugar}` : ""}`,
  ];
}

/** Abre el domicilio en Google Maps (en el celular abre la app de mapas). */
export const urlMapa = (d: Domicilio) =>
  `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(lineasDomicilio(d).join(", "))}`;