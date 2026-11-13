// Guarda en el celular quién está usando la app, para no elegirlo cada vez.
// Si el navegador no deja guardar (modo privado), funciona igual mientras la página esté abierta.

const CLAVE = "montacargas.personalId";
let enMemoria: number | null = null;

export function leerPersonalId(): number | null {
  try {
    const valor = localStorage.getItem(CLAVE);
    return valor === null ? enMemoria : Number(valor);
  } catch {
    return enMemoria;
  }
}

export function guardarPersonalId(id: number | null) {
  enMemoria = id;
  try {
    if (id === null) localStorage.removeItem(CLAVE);
    else localStorage.setItem(CLAVE, String(id));
  } catch {
    // Se queda solo en memoria
  }
}