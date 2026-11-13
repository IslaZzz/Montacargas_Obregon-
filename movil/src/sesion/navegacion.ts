import type { TipoPersonal } from "./sesionTypes";

export interface Pestana {
  ruta: string;
  etiqueta: string;
  icono: string; // Nombre de Material Symbols
}

// Pantalla a la que llega cada quien al abrir la app
export const INICIO: Record<TipoPersonal, string> = {
  OP_TRASLADO: "/traslados",
  MECANICO: "/revisiones",
};

// Pestañas de la navegación inferior. Con una sola pestaña no se muestra la barra.
// Cada historia agrega aquí su pestaña si la necesita.
export const PESTANAS: Record<TipoPersonal, Pestana[]> = {
  OP_TRASLADO: [{ ruta: "/traslados", etiqueta: "Tareas", icono: "local_shipping" }],
  MECANICO: [
    { ruta: "/revisiones", etiqueta: "Revisiones", icono: "fact_check" },
    { ruta: "/mantenimientos", etiqueta: "Mantenimientos", icono: "build" },
    { ruta: "/historial", etiqueta: "Historial", icono: "history" },
  ],
};