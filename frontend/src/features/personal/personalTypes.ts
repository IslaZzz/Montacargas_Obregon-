// Personal: operadores de traslado y mecánicos (OHU-26, 27 y 28), como lo manda /api/personal

export type TipoPersonal = "OP_TRASLADO" | "MECANICO";

export interface Personal {
  id: number;
  codigo: string; // Con el que entra a la app móvil
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  tipo: TipoPersonal;
  activo: boolean;
}

// Lo que se manda al registrar o editar (PersonalRequest en el backend)
export interface DatosPersonal {
  codigo: string;
  nombres: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  tipo: TipoPersonal;
}

export const NOMBRE_TIPO: Record<TipoPersonal, string> = {
  OP_TRASLADO: "Operador de traslado",
  MECANICO: "Mecánico",
};

export const ICONO_TIPO: Record<TipoPersonal, string> = {
  OP_TRASLADO: "local_shipping",
  MECANICO: "build",
};

export const nombreCompleto = (p: Pick<Personal, "nombres" | "apellidoPaterno" | "apellidoMaterno">) =>
  [p.nombres, p.apellidoPaterno, p.apellidoMaterno].filter(Boolean).join(" ");
