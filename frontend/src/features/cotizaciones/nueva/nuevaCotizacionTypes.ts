import type { DatosDireccionRequest } from "../../../types/comun";
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
  // Dirección de la obra, campo por campo
  calleObra: string;
  numeroObra: string;
  coloniaObra: string;
  ciudadObra: string;
  estadoObra: string;
  codigoPostalObra: string;
  referenciasObra: string;
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
  calleObra: "",
  numeroObra: "",
  coloniaObra: "",
  ciudadObra: "",
  estadoObra: "",
  codigoPostalObra: "",
  referenciasObra: "",
  contactoObra: "",
  telefonoContacto: "",
  equipos: [],
};

export interface CrearCotizacionRequest {
  clienteId: number;
  razonSocialId: number;
  fechaEntrega: string;
  fechaRecoleccion: string;
  direccionObra: DatosDireccionRequest;
  contactoObra: string;
  telefonoContacto: string;
  equipoIds: number[];
}

// La dirección de la obra como la recibe el backend, sin espacios de más
export const direccionObra = (b: BorradorCotizacion): DatosDireccionRequest => ({
  calle: b.calleObra.trim(),
  numero: b.numeroObra.trim(),
  colonia: b.coloniaObra.trim(),
  ciudad: b.ciudadObra.trim(),
  estado: b.estadoObra.trim(),
  codigoPostal: b.codigoPostalObra,
  referencias: b.referenciasObra.trim() || null,
});
