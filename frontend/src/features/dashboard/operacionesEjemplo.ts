import type { Operacion, TipoOperacion } from "./dashboardTypes";
import { claveDia } from "./dashboardUtils";

// Entregas y recolecciones de ejemplo mientras no existan los traslados.
// Las fechas se calculan a partir de hoy para que la pantalla siempre tenga algo que mostrar.
// Borrar este archivo cuando dashboardApi.listarOperaciones llame al backend.
export function operacionesDeEjemplo(hoy: Date): Operacion[] {
  const operacion = (
    rentaId: number,
    tipo: TipoOperacion,
    enDias: number,
    desde: string,
    hasta: string,
    equipos: number,
    equiposPorPreparar = 0,
  ): Operacion => {
    const dia = claveDia(
      new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + enDias),
    );
    return {
      rentaId,
      folioRenta: `#${rentaId}`,
      tipo,
      desde: `${dia}T${desde}:00`,
      hasta: `${dia}T${hasta}:00`,
      equipos,
      equiposPorPreparar,
    };
  };

  return [
    operacion(1042, "ENTREGA", 0, "10:00", "12:00", 3, 2),
    operacion(1040, "ENTREGA", 0, "13:00", "15:00", 1),
    operacion(1028, "RECOLECCION", 0, "16:00", "18:00", 2),
    operacion(1032, "ENTREGA", 1, "10:00", "12:00", 2),
    operacion(1033, "ENTREGA", 4, "10:00", "12:00", 1),
    operacion(1034, "ENTREGA", 5, "10:00", "12:00", 2),
    operacion(1030, "RECOLECCION", 8, "10:00", "12:00", 1),
  ];
}
