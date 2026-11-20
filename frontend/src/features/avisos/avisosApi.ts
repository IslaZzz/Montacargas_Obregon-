import { getApi, patchApi } from "../../services/apiClient";
import type { ApartadoProximo, AvisoDescuento } from "./avisosTypes";

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