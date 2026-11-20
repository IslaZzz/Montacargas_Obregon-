// Cómo se muestra el valor de un campo de la categoría (especificación del modelo o dato de la revisión).

/** "3,000 kg", "1,284.5 h", "3/4", "20 nov 2026"; sin dato, "—". */
export function mostrarValor(valor: string | null, tipo: string, unidad: string | null) {
  if (valor === null || valor === "") return "—";
  const texto =
    tipo === "ENTERO" || tipo === "DECIMAL"
      ? Number(valor).toLocaleString("es-MX", { maximumFractionDigits: 3 })
      : tipo === "FECHA"
        ? new Date(`${valor}T00:00`).toLocaleDateString("es-MX", { day: "numeric", month: "short", year: "numeric" })
        : valor;
  return unidad ? `${texto} ${unidad}` : texto;
}