import type { ChipStatus } from "../../components/chips";
import { formatoDiaCorto } from "../../utils/fechas";
import type { ApartadoProximo, EstadoCarta } from "./avisosTypes";

// Opciones de "Avisar con anticipación" (Figma OHU-24)
export const ANTICIPACIONES = [1, 3, 5, 7, 10];
export const ANTICIPACION_INICIAL = 7;

// Hasta cuántos días antes del inicio el aviso se marca en amarillo.
// Sacado del Figma (1 y 5 días en amarillo, 6 y 7 en verde): confirmar con el equipo.
const DIAS_URGENTE = 5;

export const textoAnticipacion = (dias: number) =>
  dias === 1 ? "1 día antes" : `${dias} días antes`;

const NOMBRE_CARTA: Record<Exclude<EstadoCarta, "FIRMADA">, string> = {
  SIN_GENERAR: "Sin generar",
  GENERADA: "Generada",
  ENVIADA: "Enviada",
};

// El Chip de carta no tiene "sin generar": se usa el gris de borrador
const CHIP_CARTA: Record<Exclude<EstadoCarta, "FIRMADA">, ChipStatus> = {
  SIN_GENERAR: "borrador",
  GENERADA: "generada",
  ENVIADA: "enviada",
};

export const nombreCarta = (a: ApartadoProximo) => NOMBRE_CARTA[a.estadoCarta];
export const chipCarta = (a: ApartadoProximo) => CHIP_CARTA[a.estadoCarta];

export const sinCarta = (a: ApartadoProximo) => a.estadoCarta === "SIN_GENERAR";
export const esUrgente = (a: ApartadoProximo) => a.diasParaInicio <= DIAS_URGENTE;

// ["MC-02", "MC-08"] → "MC-02 y MC-08"
function unirUnidades(unidades: string[]) {
  if (unidades.length <= 1) return unidades.join("");
  return `${unidades.slice(0, -1).join(", ")} y ${unidades[unidades.length - 1]}`;
}

// → "MC-09, Caterpillar P3000" (un equipo) o "MC-02 y MC-08, 2 equipos"
export function textoEquipos(a: ApartadoProximo) {
  const { equipos } = a;
  if (equipos.length === 0) return "Sin equipos asignados";
  if (equipos.length === 1) return `${equipos[0].numeroUnidad}, ${equipos[0].modelo}`;
  return `${unirUnidades(equipos.map((e) => e.numeroUnidad))}, ${equipos.length} equipos`;
}

// Solo las unidades, para el aviso de la campana: "MC-02 y MC-08"
export const unidades = (a: ApartadoProximo) =>
  unirUnidades(a.equipos.map((e) => e.numeroUnidad));

// "2026-10-08" → "jue 8 oct"
const diaCorto = (fecha: string) => {
  const texto = formatoDiaCorto(fecha);
  return texto.charAt(0).toLowerCase() + texto.slice(1);
};

// → "Inicia hoy, mié 7 oct", "Inicia mañana, jue 8 oct" o "Inicia el lun 12 oct, en 5 días"
export function textoInicio(a: ApartadoProximo) {
  const dia = diaCorto(a.fechaInicio);
  if (a.diasParaInicio <= 0) return `Inicia hoy, ${dia}`;
  if (a.diasParaInicio === 1) return `Inicia mañana, ${dia}`;
  return `Inicia el ${dia}, en ${a.diasParaInicio} días`;
}

// Qué hacer con la carta, según en qué va
export function accionCarta(a: ApartadoProximo) {
  if (a.estadoCarta === "SIN_GENERAR") return { texto: "Generar carta", icono: "note_add" };
  if (a.estadoCarta === "GENERADA") return { texto: "Enviar carta", icono: "send" };
  return { texto: "Ver carta", icono: "visibility" };
}

// Pantalla de la carta de la renta (OHU-04). Cuando exista el detalle de la renta (OHU-03), llevar ahí.
export const rutaCarta = (a: ApartadoProximo) => `/rentas/${a.rentaId}/carta`;

// Identifica un aviso para marcarlo como leído.
// Incluye el estado de la carta: si la carta avanza (por ejemplo, ya se generó), se vuelve a avisar.
export const claveAviso = (a: ApartadoProximo) => `${a.rentaId}-${a.estadoCarta}`;
