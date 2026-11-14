// Cuentas y lectura de montos de la pantalla del precio (HU-13 y HU-14)

export const IVA = 0.16;
export const ANTICIPO = 0.3;

// Hasta 7 enteros y 2 decimales
const PATRON_MONTO = /^\d{1,7}(\.\d{0,2})?$/;

export const MENSAJE_MONTO =
  "Escribe un monto válido, por ejemplo 900.00. Si no hay traslado, deja 0.00.";

// Redondea a centavos para que las sumas no arrastren decimales
export const redondear = (pesos: number) => Math.round(pesos * 100) / 100;

// Texto del campo -> número. Vacío cuenta como 0 (el traslado es opcional); null si no es un monto válido
export function leerMonto(texto: string): number | null {
  const limpio = texto.replace(/[,\s$]/g, "");
  if (limpio === "") return 0;
  return PATRON_MONTO.test(limpio) ? Number(limpio) : null;
}

export function calcularPrecio(
  subtotalEquipos: number,
  trasladoEntrega: number,
  trasladoRecoleccion: number,
) {
  const subtotal = redondear(subtotalEquipos + trasladoEntrega + trasladoRecoleccion);
  const iva = redondear(subtotal * IVA);
  const total = redondear(subtotal + iva);
  const anticipo = redondear(total * ANTICIPO);
  return { subtotal, iva, total, anticipo };
}

// Deja pasar solo dígitos y un punto: hasta 7 enteros y 2 decimales.
// Se usa mientras se escribe, así que también limpia lo que se pegue.
export function limpiarMonto(texto: string): string {
  const soloValidos = texto.replace(/[^\d.]/g, "");
  const [enteros, ...resto] = soloValidos.split(".");
  const parteEntera = enteros.slice(0, 7);

  // Sin punto, solo los enteros
  if (resto.length === 0) return parteEntera;

  // Con punto, se conserva el primero y se ignoran los demás
  const decimales = resto.join("").slice(0, 2);
  return `${parteEntera === "" ? "0" : parteEntera}.${decimales}`;
}