import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Icono from "./Icono";

// Cuánto tiempo se queda visible
const DURACION = 4000;

/** Para mostrar un aviso en la pantalla a la que se regresa: navigate("/revisiones", conNotificacion("EQ-014 pasó al historial")) */
export const conNotificacion = (texto: string) => ({ state: { notificacion: texto } });

/** Aviso oscuro sobre la navegación inferior. Lo muestra Pantalla cuando se llega con conNotificacion. */
export default function Notificacion() {
  const ubicacion = useLocation();
  const navigate = useNavigate();
  const recibido = (ubicacion.state as { notificacion?: string } | null)?.notificacion ?? null;
  const [texto, setTexto] = useState<string | null>(recibido);

  // Lo toma y lo quita del historial para que no vuelva a salir al regresar o recargar
  useEffect(() => {
    if (!recibido) return;
    setTexto(recibido);
    navigate(ubicacion.pathname + ubicacion.search, { replace: true, state: null });
  }, [recibido, navigate, ubicacion.pathname, ubicacion.search]);

  // Se oculta solo
  useEffect(() => {
    if (!texto) return;
    const temporizador = setTimeout(() => setTexto(null), DURACION);
    return () => clearTimeout(temporizador);
  }, [texto]);

  if (!texto) return null;
  return (
    <div className="notificacion" role="status">
      <Icono nombre="check_circle" />
      <span>{texto}</span>
    </div>
  );
}