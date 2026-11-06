// Pantalla de inicio, como la manda GET /api/inicio

export type TipoOperacion = "ENTREGA" | "RECOLECCION" | "REVISION" | "MANTENIMIENTO";

export type TipoAviso =
  | "TRASLADO_ATRASADO"
  | "TRASLADO_SIN_OPERADOR"
  | "ENTREGA_EN_RIESGO"
  | "MANTENIMIENTO_VENCIDO"
  | "FALTA_RECOLECCION";

// Lo que queda por hacer hoy
export interface Pendientes {
  entregas: number;
  recolecciones: number;
  revisiones: number;
  mantenimientos: number;
}

// Algo que hay que resolver. Lleva a la renta si tiene rentaId; si no, a la ruta
export interface Aviso {
  tipo: TipoAviso;
  titulo: string;
  detalle: string;
  rentaId: number | null;
  ruta: string | null;
}

// Un traslado, una revisión o un mantenimiento con fecha
export interface Operacion {
  tipo: TipoOperacion;
  titulo: string; // "Entrega", "Revisión de salida"…
  detalle: string; // "Renta R-2072 · TR-0004"
  desde: string; // "2026-10-08T10:00:00"
  hasta: string | null; // Null si no es un horario (revisiones y mantenimientos)
  rentaId: number | null;
  ruta: string;
}

export interface Inicio {
  hoy: string; // "2026-11-06"
  pendientes: Pendientes;
  avisos: Aviso[]; // Del más urgente al menos urgente
  operaciones: Operacion[]; // Desde hoy, ordenadas por fecha
}