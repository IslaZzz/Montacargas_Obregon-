// Inicio de sesión (HU-28)

export type RolUsuario = "GERENTE" | "AUXILIAR";

// El usuario con la sesión abierta
export interface Sesion {
  nombre: string;
  correo: string;
  rol: RolUsuario;
}

export interface DatosInicioSesion {
  correo: string;
  contrasena: string;
}

// Por qué se llegó a «Iniciar sesión»: se cerró la sesión o se quiso abrir una pantalla sin ella
export interface EstadoInicioSesion {
  motivo?: "cerrada" | "requerida";
  desde?: string; // Ruta que se quiso abrir; al entrar se regresa ahí
}
