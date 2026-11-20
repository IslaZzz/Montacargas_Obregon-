// Números como los escribe el usuario.

/** Lee un número escrito por el usuario ("1,284.5" o "1284,5"). null si no es número. */
export function leerNumero(texto: string): number | null {
  const limpio = texto.trim().replace(/\s/g, "");
  if (!limpio) return null;
  // Si trae coma y punto, la coma es de miles; si solo trae coma, es el decimal
  const normal = limpio.includes(".") ? limpio.replace(/,/g, "") : limpio.replace(",", ".");
  const numero = Number(normal);
  return Number.isFinite(numero) ? numero : null;
}