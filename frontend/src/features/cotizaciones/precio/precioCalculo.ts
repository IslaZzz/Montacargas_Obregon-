// Cuentas y lectura de montos de la pantalla del precio (HU-13, HU-14 y HU-15)
import type { TipoDescuento } from "../cotizacionesTypes";

export const IVA = 0.16;
export const ANTICIPO = 0.3;

// Hasta 7 enteros y 2 decimales
const PATRON_MONTO = /^\d{1,7}(\.\d{0,2})?$/;

export const MENSAJE_MONTO =
  "Escribe un monto válido, por ejemplo 900.00. Si no hay traslado, deja 0.00.";

export const MENSAJE_DESCUENTO = "Descuento inválido";
export const MENSAJE_MOTIVO = "Escribe el motivo del descuento";

// Redondea a centavos para que las sumas no arrastren decimales
export const redondear = (pesos: number) => Math.round(pesos * 100) / 100;

// Texto del campo -> número. Vacío cuenta como 0 (el traslado es opcional); null si no es un monto válido
export function leerMonto(texto: string): number | null {
  const limpio = texto.replace(/[,\s$]/g, "");
  if (limpio === "") return 0;
  return PATRON_MONTO.test(limpio) ? Number(limpio) : null;
}

/**
 * subtotal: equipos + traslados (antes del descuento).
 * conDescuento: subtotal menos el descuento; sobre él se calcula el IVA.
 */
export function calcularPrecio(
  subtotalEquipos: number,
  trasladoEntrega: number,
  trasladoRecoleccion: number,
  descuento = 0,
) {
  const subtotal = redondear(subtotalEquipos + trasladoEntrega + trasladoRecoleccion);
  const conDescuento = redondear(subtotal - descuento);
  const iva = redondear(conDescuento * IVA);
  const total = redondear(conDescuento + iva);
  const anticipo = redondear(total * ANTICIPO);
  return { subtotal, conDescuento, iva, total, anticipo };
}

/**
 * Pesos que se descuentan, o null si el valor no es válido (HU-15, criterio 2).
 * Porcentaje: mayor a 0 y menor a 100. Monto: mayor a 0 y menor al subtotal.
 */
export function calcularDescuento(
  tipo: TipoDescuento,
  texto: string,
  subtotal: number,
): number | null {
  if (tipo === "NINGUNO") return 0;

  if (texto === "" || texto === ".") return null;
  const valor = Number(texto);
  if (!(valor > 0)) return null;

  if (tipo === "PORCENTAJE") {
    return valor < 100 ? redondear((subtotal * valor) / 100) : null;
  }
  return valor < subtotal ? redondear(valor) : null;
}

// Deja pasar solo dígitos y un punto: hasta 7 enteros y 2 decimales.
// Se usa mientras se escribe, así que también limpia lo que se pegue.
export function limpiarMonto(texto: string): string {
  return limpiarNumero(texto, 7);
}

// Igual que limpiarMonto, pero con hasta 3 enteros (para el porcentaje)
export function limpiarPorcentaje(texto: string): string {
  return limpiarNumero(texto, 3);
}

function limpiarNumero(texto: string, maxEnteros: number): string {
  const soloValidos = texto.replace(/[^\d.]/g, "");
  const [enteros, ...resto] = soloValidos.split(".");
  const parteEntera = enteros.slice(0, maxEnteros);

  // Sin punto, solo los enteros
  if (resto.length === 0) return parteEntera;

  // Con punto, se conserva el primero y se ignoran los demás
  const decimales = resto.join("").slice(0, 2);
  return `${parteEntera === "" ? "0" : parteEntera}.${decimales}`;
}