import { createContext, useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { registrarAlSesionInvalida } from "../services/api";
import { borrarSesion, guardarSesion, leerSesion } from "./almacen";
import { cerrarSesionEnServidor, quienSoy } from "./sesionApi";
import type { Personal, SesionIniciada } from "./sesionTypes";

interface Sesion {
  personal: Personal | null;
  venceEn: string | null; // Hora en que termina la jornada de esta sesión (ISO, UTC)
  cargando: boolean; // Mientras se confirma la sesión guardada
  entrar: (sesion: SesionIniciada) => void;
  salir: () => Promise<void>;
}

export const SesionContext = createContext<Sesion | null>(null);

const MENSAJE_VENCIDA = "Tu sesión venció. Ingresa tu código para iniciar una nueva jornada.";
// setTimeout no admite más de ~24.8 días; una jornada dura horas, pero por si se configura más
const MAXIMO_TIMEOUT = 2_147_483_647;

/**
 * Quién usa la app. Entra con su código, el backend le da un token que dura una jornada y que
 * se manda en cada petición. El servidor decide si sigue vigente; aquí solo se corta antes de tiempo
 * para no esperar a que una petición falle, y se limpia todo cuando el backend responde 401.
 */
export function SesionProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  // useNavigate cambia con cada ruta; se guarda en una referencia para que expirar sea siempre la misma función
  const navegar = useRef(navigate);
  useEffect(() => {
    navegar.current = navigate;
  }, [navigate]);

  const [personal, setPersonal] = useState<Personal | null>(null);
  const [venceEn, setVenceEn] = useState<string | null>(null);
  const [cargando, setCargando] = useState(() => leerSesion() !== null);

  // Borra la sesión del celular y manda a /entrar con el motivo
  const expirar = useCallback((mensaje: string) => {
    borrarSesion();
    setPersonal(null);
    setVenceEn(null);
    const desde = window.location.pathname;
    navegar.current("/entrar", { replace: true, state: { aviso: mensaje, desde: desde === "/entrar" ? undefined : desde } });
  }, []);

  // Al abrir la app: si hay sesión guardada se confirma con el servidor, que es la fuente de verdad
  useEffect(() => {
    const guardada = leerSesion();
    if (guardada === null) return;

    // Corte preventivo: ya pasó la hora de vencimiento, ni se pregunta al servidor
    if (Date.now() >= Date.parse(guardada.venceEn)) {
      queueMicrotask(() => {
        expirar(MENSAJE_VENCIDA);
        setCargando(false);
      });
      return;
    }

    quienSoy()
      .then((vigente) => {
        setPersonal(vigente.personal);
        setVenceEn(vigente.venceEn);
      })
      // Un 401 ya lo atendió el manejador global; sin conexión se conserva el token guardado
      .catch(() => undefined)
      .finally(() => setCargando(false));
  }, [expirar]);

  // Un 401 con sesión (venció, la cerraron o entró en otro celular) saca a la persona de la app
  useEffect(() => {
    registrarAlSesionInvalida((mensaje, tokenEnviado) => {
      // Una respuesta tardía de un token anterior no debe cerrar la sesión nueva
      if (leerSesion()?.token !== tokenEnviado) return;
      expirar(mensaje);
    });
    return () => registrarAlSesionInvalida(null);
  }, [expirar]);

  // Si la app se queda abierta, cierra la sesión justo al vencer (y al volver a verla, por si el celular durmió)
  useEffect(() => {
    if (venceEn === null) return;
    const vence = Date.parse(venceEn);

    const comprobar = () => {
      if (Date.now() >= vence) expirar(MENSAJE_VENCIDA);
    };
    const espera = Math.min(Math.max(vence - Date.now(), 0), MAXIMO_TIMEOUT);
    const temporizador = window.setTimeout(comprobar, espera);
    const alCambiarVisibilidad = () => {
      if (document.visibilityState === "visible") comprobar();
    };
    document.addEventListener("visibilitychange", alCambiarVisibilidad);

    return () => {
      window.clearTimeout(temporizador);
      document.removeEventListener("visibilitychange", alCambiarVisibilidad);
    };
  }, [venceEn, expirar]);

  const entrar = useCallback((sesion: SesionIniciada) => {
    guardarSesion({ token: sesion.token, venceEn: sesion.venceEn });
    setPersonal(sesion.personal);
    setVenceEn(sesion.venceEn);
  }, []);

  // Cierra en el servidor y, pase lo que pase (incluso sin red), en el celular
  const salir = useCallback(async () => {
    try {
      await cerrarSesionEnServidor();
    } catch {
      // Sin conexión o ya vencida: igual se cierra aquí
    } finally {
      borrarSesion();
      setPersonal(null);
      setVenceEn(null);
    }
  }, []);

  const valor = useMemo(
    () => ({ personal, venceEn, cargando, entrar, salir }),
    [personal, venceEn, cargando, entrar, salir],
  );
  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>;
}
