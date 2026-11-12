import type { RolUsuario } from "./sesionTypes";

// Pantalla a la que se llega al entrar, si no se venía de otra
export const RUTA_AL_ENTRAR = "/cotizaciones";

// "Auxiliar" o "Gerencia", para el encabezado
export function rolCorto(rol: RolUsuario): string {
  return rol === "GERENTE" ? "Gerencia" : "Auxiliar";
}

// Puesto completo, para el menú lateral y el menú de usuario
export function puesto(rol: RolUsuario): string {
  return rol === "GERENTE" ? "Gerencia" : "Auxiliar de oficina";
}

// El rol como lo espera MenuLateral
export function rolMenu(rol: RolUsuario): "gerencia" | "auxiliar" {
  return rol === "GERENTE" ? "gerencia" : "auxiliar";
}

// A dónde ir al entrar: la pantalla que se quiso abrir, si es una ruta interna
export function rutaAlEntrar(desde?: string): string {
  if (!desde || desde === "/" || !desde.startsWith("/") || desde.startsWith("//")) {
    return RUTA_AL_ENTRAR;
  }
  return desde;
}

// Nombre de las pantallas como aparecen en el menú lateral
const pantallas: Record<string, string> = {
  dashboard: "Inicio",
  rentas: "Rentas",
  clientes: "Clientes",
  equipos: "Equipos",
  disponibilidad: "Disponibilidad",
  cotizaciones: "Cotizaciones",
  revisiones: "Revisiones",
  traslados: "Traslados",
  mantenimientos: "Mantenimientos",
  "apartados-proximos": "Apartados próximos",
};

// "/cotizaciones/12" -> "Cotizaciones". Null si no es una pantalla del menú
export function nombrePantalla(ruta?: string): string | null {
  const seccion = ruta?.split(/[/?#]/)[1] ?? "";
  return pantallas[seccion] ?? null;
}
