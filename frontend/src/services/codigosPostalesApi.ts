import { getApi } from "./apiClient";
import type { CodigoPostal } from "../types/comun";

export const buscarCodigoPostal = (codigoPostal: string) =>
  getApi<CodigoPostal>(`/codigos-postales/${codigoPostal}`);
