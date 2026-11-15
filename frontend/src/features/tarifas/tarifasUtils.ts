// 2500 pasa a $2,500.00
const formatoPesos = new Intl.NumberFormat('es-MX', {
  style: 'currency',
  currency: 'MXN',
})
export const formatearPesos = (valor: number) => formatoPesos.format(valor)

// 2500 pasa a 2500.00, para mostrarla dentro del campo (sin comas, porque el campo no las permite)
export const formatearParaCampo = (valor: number) => valor.toFixed(2)

export const MENSAJE_TARIFA = 'Captura una tarifa mayor a cero'

// Hasta 8 enteros, un punto y hasta 2 decimales. También acepta valores a medias
// mientras se escribe, como "" o "25." o "2500.5"
const FORMATO_EN_CAPTURA = /^\d{0,8}(\.\d{0,2})?$/

/**
 * Limpia lo que se escribió en el campo.
 * Quita todo lo que no sea número o punto (también las comas si se pega "2,500.00").
 * Si el resultado no cumple el formato, devuelve null y el campo se queda como estaba.
 */
export function limpiarTarifa(valor: string): string | null {
  const limpio = valor.replace(/[^\d.]/g, '')
  return FORMATO_EN_CAPTURA.test(limpio) ? limpio : null
}

// Revisa la tarifa al guardar. El formato ya lo cuida limpiarTarifa;
// aquí solo falta revisar que no esté vacía ni sea cero.
export function leerTarifa(
  texto: string,
): { tarifa: number } | { error: string } {
  const tarifa = Number(texto)
  if (texto === '' || texto === '.' || !(tarifa > 0)) {
    return { error: MENSAJE_TARIFA }
  }
  return { tarifa }
}