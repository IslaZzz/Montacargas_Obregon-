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

// GET /api/avisos/descuentos: respuesta de gerencia a un descuento que el usuario pidió
export interface AvisoDescuento {
  id: number;
  cotizacionId: number;
  folio: string;
  cliente: string;
  resultado: "AUTORIZADA" | "RECHAZADA";
  comentario: string | null;
  respondidaPor: string;
  fecha: string;
  leido: boolean;
}

// GET /api/avisos/operacion: lo que pasa en los traslados
export type TipoAvisoOperacion =
  | "TRASLADO_INICIADO"
  | "ENTREGA_REGISTRADA"
  | "RECOLECCION_CONFIRMADA"
  | "EQUIPOS_RECIBIDOS"
  | "TAREA_REASIGNADA";

export interface AvisoOperacion {
  id: number;
  tipo: TipoAvisoOperacion;
  titulo: string; // "Traslado iniciado: TR-0087"
  detalle: string;
  rentaId: number | null;
  fecha: string;
  leido: boolean;
}

// GET /api/avisos/recolecciones-proximas: renta en curso por terminar, sin recolección confirmada (OHU-25)
export interface RecoleccionProxima {
  rentaId: number;
  folio: string;
  cliente: string;
  domicilio: string;
  equipos: string[]; // ["MC-021", "RT-007"]
  fechaFin: string; // "2026-11-24"
  diasParaFin: number; // 0 = hoy, 1 = mañana; negativo si ya venció
  vencida: boolean;
  recoleccionDesde: string | null; // Vacío si todavía no tiene horario
  trasladoFolio: string | null;
}