import type { DatosInicioSesion, Sesion } from "./sesionTypes";

// Usa fetch directo: aquí un 401 es una respuesta esperada, no una sesión vencida

// La sesión abierta, o null si no hay
export async function obtenerSesion(): Promise<Sesion | null> {
  const respuesta = await fetch("/api/sesion");
  if (respuesta.status === 401) return null;
  if (!respuesta.ok) throw new Error(`Error ${respuesta.status}`);
  return respuesta.json() as Promise<Sesion>;
}

// Abre la sesión. Responde null si el correo o la contraseña no coinciden
export async function iniciarSesion(datos: DatosInicioSesion): Promise<Sesion | null> {
  const respuesta = await fetch("/api/sesion", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(datos),
  });
  if (respuesta.status === 401) return null;
  if (!respuesta.ok) throw new Error(`Error ${respuesta.status}`);
  return respuesta.json() as Promise<Sesion>;
}

export async function cerrarSesion(): Promise<void> {
  await fetch("/api/sesion", { method: "DELETE" });
}
