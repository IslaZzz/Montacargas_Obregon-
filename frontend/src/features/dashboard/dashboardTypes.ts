// Tipos de la pantalla de inicio

export type TipoOperacion = "ENTREGA" | "RECOLECCION";

// Una entrega o recolección de una renta.
// Todavía no viene del backend porque faltan los traslados.
export interface Operacion {
  rentaId: number;
  folioRenta: string;
  tipo: TipoOperacion;
  desde: string; // "2026-10-08T10:00:00"
  hasta: string;
  equipos: number;
  equiposPorPreparar: number;
}

// Lo que se usa de GET /api/revisiones: una fila por renta y tipo de revisión
export interface RevisionesRenta {
  clave: string; // "12-SALIDA"
  rentaId: number;
  folioRenta: string;
  tipo: "SALIDA" | "ENTRADA";
  fechaProgramada: string | null;
  estado: string;
  revisadas: number; // Revisiones completadas
  total: number; // Sin contar las canceladas
}
