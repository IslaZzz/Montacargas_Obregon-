import { getApi, patchApi } from "../../services/apiClient";
import type { ApartadoProximo, AvisoDescuento, AvisoOperacion, RecoleccionProxima } from "./avisosTypes";

// Rentas que empiezan entre hoy y dentro de `dias` días y no tienen la carta firmada
export const listarApartadosProximos = (dias: number) =>
  getApi<ApartadoProximo[]>(`/avisos/apartados-proximos?dias=${dias}`);

// Respuestas de gerencia a los descuentos que pidió el usuario
export const listarAvisosDescuento = () =>
  getApi<AvisoDescuento[]>("/avisos/descuentos");

// Los marca como leídos y devuelve la lista actualizada
export const marcarAvisosDescuentoLeidos = () =>
  patchApi<AvisoDescuento[]>("/avisos/descuentos/leidos", {});

// Marca un aviso como leído y devuelve la lista actualizada
export const marcarAvisoDescuentoLeido = (id: number) =>
  patchApi<AvisoDescuento[]>(`/avisos/descuentos/${id}/leido`, {});

// Lo que pasa en los traslados: iniciados, entregas, recolecciones y equipos recibidos
export const listarAvisosOperacion = () =>
  getApi<AvisoOperacion[]>("/avisos/operacion");

export const marcarAvisosOperacionLeidos = () =>
  patchApi<AvisoOperacion[]>("/avisos/operacion/leidos", {});

export const marcarAvisoOperacionLeido = (id: number) =>
  patchApi<AvisoOperacion[]>(`/avisos/operacion/${id}/leido`, {});


// Rentas en curso que terminan entre hoy y dentro de `dias` días (o ya terminaron) sin recolección confirmada
export const listarRecoleccionesProximas = (dias: number) =>
  getApi<RecoleccionProxima[]>(`/avisos/recolecciones-proximas?dias=${dias}`);