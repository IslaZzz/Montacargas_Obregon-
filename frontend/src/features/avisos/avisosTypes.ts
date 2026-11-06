// Tipos de los avisos de apartado próximo (OHU-24)

export type EstadoCarta = "SIN_GENERAR" | "GENERADA" | "ENVIADA" | "FIRMADA";

export interface EquipoApartado {
  numeroUnidad: string;
  modelo: string; // "Toyota 8FGU25"
}

// GET /api/avisos/apartados-proximos: renta programada que empieza pronto, sin carta firmada
export interface ApartadoProximo {
  rentaId: number;
  folio: string;
  cliente: string;
  equipos: EquipoApartado[];
  fechaInicio: string; // "2026-10-08"
  entregaDesde: string | null;
  diasParaInicio: number; // 0 = hoy, 1 = mañana
  estadoCarta: Exclude<EstadoCarta, "FIRMADA">;
}
