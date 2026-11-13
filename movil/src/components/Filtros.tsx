export interface OpcionFiltro<T extends string> {
  valor: T;
  etiqueta: string;
}

interface Props<T extends string> {
  opciones: OpcionFiltro<T>[];
  seleccionado: T;
  onCambio: (valor: T) => void;
  etiqueta: string; // Para lectores de pantalla: "Tipo de traslado"
  ancho?: boolean; // Reparte el ancho (Todos / Salida / Regreso); si no, cada botón mide su texto
}

/** Botones para filtrar una lista: Entregas / Recolecciones, Todos / En curso / Nuevos… */
export default function Filtros<T extends string>({ opciones, seleccionado, onCambio, etiqueta, ancho = true }: Props<T>) {
  return (
    <div className={`filtros${ancho ? " filtros-ancho" : ""}`} role="group" aria-label={etiqueta}>
      {opciones.map((o) => (
        <button
          key={o.valor}
          type="button"
          className="filtro"
          aria-pressed={o.valor === seleccionado}
          onClick={() => onCambio(o.valor)}
        >
          {o.etiqueta}
        </button>
      ))}
    </div>
  );
}