// Guarda en el celular la sesión de la jornada (token y hora en que vence), para no escribir el
// código cada vez que se abre la app. Si el navegador no deja guardar (modo privado), funciona
// igual mientras la página esté abierta.

export interface SesionGuardada {
  token: string;
  venceEn: string; // Instante ISO-8601 en UTC, por ejemplo "2026-11-20T03:00:00Z"
}

const CLAVE = "montacargas.sesionMovil";
// Llave de cuando la app solo guardaba el id de la persona; ya no se usa
const CLAVE_ANTERIOR = "montacargas.personalId";
let enMemoria: SesionGuardada | null = null;

function limpiarLlaveAnterior() {
  try {
    localStorage.removeItem(CLAVE_ANTERIOR);
  } catch {
    // Sin almacenamiento, no hay nada que limpiar
  }
}
limpiarLlaveAnterior();

function esSesion(valor: unknown): valor is SesionGuardada {
  const s = valor as Partial<SesionGuardada> | null;
  return typeof s?.token === "string" && s.token !== "" && typeof s.venceEn === "string" && s.venceEn !== "";
}

export function leerSesion(): SesionGuardada | null {
  try {
    const texto = localStorage.getItem(CLAVE);
    if (texto === null) return enMemoria;
    const valor: unknown = JSON.parse(texto);
    if (esSesion(valor)) return valor;
    localStorage.removeItem(CLAVE); // Quedó algo que no es una sesión
    return enMemoria;
  } catch {
    return enMemoria;
  }
}

export function guardarSesion(sesion: SesionGuardada) {
  enMemoria = sesion;
  try {
    localStorage.setItem(CLAVE, JSON.stringify(sesion));
  } catch {
    // Se queda solo en memoria
  }
}

export function borrarSesion() {
  enMemoria = null;
  try {
    localStorage.removeItem(CLAVE);
  } catch {
    // Ya no hay nada en memoria
  }
}
