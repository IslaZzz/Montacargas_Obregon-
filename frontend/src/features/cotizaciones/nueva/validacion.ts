import type { BorradorCotizacion, CampoTexto, Errores } from "./nuevaCotizacionTypes";
import { fechaHoraIso } from "./calculo";

interface Regla {
  campo: keyof BorradorCotizacion;
  mensaje: string;
  incumple: (b: BorradorCotizacion) => boolean;
}

const obligatorio = (campo: CampoTexto, mensaje: string): Regla => ({
  campo,
  mensaje,
  incumple: (b) => b[campo].trim() === "",
});

const conFormato = (campo: CampoTexto, patron: RegExp, mensaje: string): Regla => ({
  campo,
  mensaje,
  incumple: (b) => !patron.test(b[campo]),
});

const recoleccionAntesDeEntrega = (b: BorradorCotizacion): boolean =>
  b.fechaEntrega !== "" &&
  b.fechaRecoleccion !== "" &&
  fechaHoraIso(b.fechaRecoleccion, b.horaRecoleccion) < fechaHoraIso(b.fechaEntrega, b.horaEntrega);

const REGLAS_POR_PASO: Regla[][] = [
  [
    { campo: "cliente", mensaje: "Elige un cliente", incumple: (b) => b.cliente === null },
    {
      campo: "razonSocialId",
      mensaje: "Elige la razón social para facturar",
      incumple: (b) => b.cliente !== null && b.razonSocialId === null,
    },
  ],
  [
    obligatorio("fechaEntrega", "Elige la fecha de entrega"),
    obligatorio("horaEntrega", "Elige la hora de entrega"),
    obligatorio("fechaRecoleccion", "Elige la fecha de recolección"),
    obligatorio("horaRecoleccion", "Elige la hora de recolección"),
    {
      campo: "fechaRecoleccion",
      mensaje: "La recolección no puede ser antes de la entrega",
      incumple: recoleccionAntesDeEntrega,
    },
    obligatorio("calleObra", "Escribe la calle"),
    obligatorio("numeroObra", "Escribe el número"),
    obligatorio("coloniaObra", "Escribe la colonia"),
    obligatorio("ciudadObra", "Escribe la ciudad"),
    obligatorio("estadoObra", "Escribe el estado"),
    obligatorio("codigoPostalObra", "Escribe el código postal"),
    conFormato("codigoPostalObra", /^\d{5}$/, "El código postal debe tener 5 dígitos"),
    obligatorio("contactoObra", "Escribe quién recibe en la obra"),
    conFormato("telefonoContacto", /^\d{10}$/, "El teléfono debe tener 10 dígitos"),
  ],
  [{ campo: "equipos", mensaje: "Agrega al menos un equipo", incumple: (b) => b.equipos.length === 0 }],
  [],
];

export function validarPaso(paso: number, borrador: BorradorCotizacion): Errores {
  const errores: Errores = {};
  for (const regla of REGLAS_POR_PASO[paso] ?? []) {
    if (errores[regla.campo] === undefined && regla.incumple(borrador)) {
      errores[regla.campo] = regla.mensaje;
    }
  }
  return errores;
}

const CAMPO_DEL_BACKEND: Record<string, keyof BorradorCotizacion> = {
  clienteId: "cliente",
  razonSocialId: "razonSocialId",
  fechaEntrega: "fechaEntrega",
  fechaRecoleccion: "fechaRecoleccion",
  direccionObra: "calleObra",
  "direccionObra.calle": "calleObra",
  "direccionObra.numero": "numeroObra",
  "direccionObra.colonia": "coloniaObra",
  "direccionObra.ciudad": "ciudadObra",
  "direccionObra.estado": "estadoObra",
  "direccionObra.codigoPostal": "codigoPostalObra",
  "direccionObra.referencias": "referenciasObra",
  contactoObra: "contactoObra",
  telefonoContacto: "telefonoContacto",
  equipoIds: "equipos",
};

export function erroresDelBackend(errores: Record<string, string>): Errores {
  const resultado: Errores = {};
  for (const [campo, mensaje] of Object.entries(errores)) {
    const propio = CAMPO_DEL_BACKEND[campo];
    if (propio) resultado[propio] = mensaje;
  }
  return resultado;
}

export function pasoConErrores(errores: Errores): number | null {
  const paso = REGLAS_POR_PASO.findIndex((reglas) => reglas.some((r) => errores[r.campo] !== undefined));
  return paso === -1 ? null : paso;
}

export const hayErrores = (errores: Errores): boolean => Object.keys(errores).length > 0;
