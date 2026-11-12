import { getApi } from "../../services/apiClient";
import type { Renta } from "./estadosRentaTypes";

export const listarRentas = () =>
  getApi<Renta[]>("/rentas");
