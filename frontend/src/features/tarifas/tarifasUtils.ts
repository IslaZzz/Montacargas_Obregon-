// 2500 pasa a $2,500.00
const formatoPesos = new Intl.NumberFormat('es-MX', {
  style: 'currency',
  currency: 'MXN',
})
export const formatearPesos = (valor: number) => formatoPesos.format(valor)

// 2500 pasa a 2,500.00, para mostrarla dentro del campo
export const formatearParaCampo = (valor: number) =>
  valor.toLocaleString('es-MX', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })

export const MENSAJE_TARIFA = 'Captura una tarifa mayor a cero'

// La columna tarifa_dia es DECIMAL(10,2): hasta 8 enteros
const TARIFA_MAXIMA = 99_999_999.99

// Revisa lo que se escribió en el campo.
// Devuelve la tarifa como número o el mensaje de error.
export function leerTarifa(
  texto: string,
): { tarifa: number } | { error: string } {
  // Se aceptan las comas de miles: "2,500.00" se lee como 2500.00
  const limpio = texto.replace(/,/g, '').trim()

  // Vacío, negativo o con letras
  if (!/^(\d+(\.\d*)?|\.\d+)$/.test(limpio)) return { error: MENSAJE_TARIFA }

  const tarifa = Number(limpio)
  if (tarifa <= 0) return { error: MENSAJE_TARIFA }

  const decimales = limpio.split('.')[1] ?? ''
  if (decimales.length > 2)
    return { error: 'La tarifa admite máximo 2 decimales' }

  if (tarifa > TARIFA_MAXIMA)
    return { error: 'La tarifa no puede pasar de $99,999,999.99' }

  return { tarifa }
}