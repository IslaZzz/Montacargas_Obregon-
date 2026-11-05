import type { Cliente } from "../../clientes/clientesTypes";

export interface EquipoCatalogo {
  id: number;
  numeroUnidad: string;
  numeroSerie: string;
  anioCompra: number;
  estado: string;
  etiquetas: string[];
  modelo: {
    id: number;
    nombre: string;
    marca: string;
    codigo: string;
    tarifaDia: number;
    categoria: string;
  };
}

export interface BorradorCotizacion {
  cliente: Cliente | null;
  razonSocialId: number | null;
  fechaEntrega: string;
  horaEntrega: string;
  fechaRecoleccion: string;
  horaRecoleccion: string;
  direccionObra: string;
  contactoObra: string;
  telefonoContacto: string;
  equipos: EquipoCatalogo[];
}

export type CampoTexto = {
  [K in keyof BorradorCotizacion]: BorradorCotizacion[K] extends string ? K : never;
}[keyof BorradorCotizacion];

export type Errores = Partial<Record<keyof BorradorCotizacion, string>>;

export const borradorVacio: BorradorCotizacion = {
  cliente: null,
  razonSocialId: null,
  fechaEntrega: "",
  horaEntrega: "09:00",
  fechaRecoleccion: "",
  horaRecoleccion: "17:00",
  direccionObra: "",
  contactoObra: "",
  telefonoContacto: "",
  equipos: [],
};

export interface CrearCotizacionRequest {
  clienteId: number;
  razonSocialId: number;
  fechaEntrega: string;
  fechaRecoleccion: string;
  direccionObra: string;
  contactoObra: string;
  telefonoContacto: string;
  equipoIds: number[];
}
