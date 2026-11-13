import { useId, useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import { useTitulo } from "../../hooks/useTitulo";
import type { EstadoInicioSesion } from "./sesionTypes";
import { nombrePantalla, rutaAlEntrar } from "./sesionUtils";
import { useSesion } from "./useSesion";
import "../../styles/sesion/iniciar-sesion.css";

type ErrorInicio = "vacios" | "incorrectos" | "conexion";

const beneficios = [
  "Cotizaciones con precio, traslado y descuento",
  "Rentas, entregas y recolecciones al día",
  "Revisiones de equipo antes de cada salida",
];

// Aviso de arriba del formulario: un error o el motivo por el que se llegó aquí
function AvisoInicio({ error, estado }: { error: ErrorInicio | null; estado: EstadoInicioSesion }) {
  if (error === "vacios") {
    return (
      <Aviso tipo="error" titulo="Captura tu correo y contraseña">
        Los dos campos son obligatorios para entrar.
      </Aviso>
    );
  }
  if (error === "incorrectos") {
    return (
      <Aviso tipo="error" titulo="Correo o contraseña incorrectos">
        Revisa tus datos e inténtalo otra vez. Si no la recuerdas, pídele una nueva a la oficina.
      </Aviso>
    );
  }
  if (error === "conexion") {
    return (
      <Aviso tipo="error" titulo="No se pudo entrar">
        No hubo respuesta del sistema. Inténtalo otra vez en un momento.
      </Aviso>
    );
  }
  if (estado.motivo === "cerrada") {
    return (
      <Aviso tipo="exito" titulo="Cerraste tu sesión">
        Para volver a entrar escribe tu correo y tu contraseña.
      </Aviso>
    );
  }
  // Solo se avisa si se quiso abrir una pantalla en particular, no al abrir el sistema
  if (estado.motivo === "requerida" && rutaAlEntrar(estado.desde) === estado.desde) {
    const pantalla = nombrePantalla(estado.desde);
    return (
      <Aviso tipo="advertencia" icono="lock" titulo="Inicia sesión para continuar">
        {pantalla
          ? `Intentaste abrir «${pantalla}» sin sesión. Al entrar te llevamos a esa pantalla.`
          : "Intentaste abrir una pantalla sin sesión. Al entrar te llevamos a ella."}
      </Aviso>
    );
  }
  return null;
}

// Pantalla «Iniciar sesión» (HU-28)
export default function IniciarSesion() {
  useTitulo("Iniciar sesión");
  const { sesion, entrar } = useSesion();
  const estado = (useLocation().state ?? {}) as EstadoInicioSesion;
  const id = useId();

  const [correo, setCorreo] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [mostrar, setMostrar] = useState(false);
  const [error, setError] = useState<ErrorInicio | null>(null);
  const [enviando, setEnviando] = useState(false);

  // Mientras se revisa si ya había sesión no se muestra nada
  if (sesion === undefined) return null;
  // Con sesión abierta se va directo a la pantalla
  if (sesion) return <Navigate to={rutaAlEntrar(estado.desde)} replace />;

  async function alEntrar(e: FormEvent) {
    e.preventDefault();
    if (!correo.trim() || !contrasena) {
      setError("vacios");
      return;
    }
    setEnviando(true);
    try {
      const entro = await entrar({ correo: correo.trim(), contrasena });
      // Si entró, este componente se redirige solo al cambiar la sesión
      if (!entro) setError("incorrectos");
    } catch {
      setError("conexion");
    } finally {
      setEnviando(false);
    }
  }

  // Con campos vacíos se marcan en rojo los que faltan
  const correoFalta = error === "vacios" && !correo.trim();
  const contrasenaFalta = error === "vacios" && !contrasena;

  return (
    <div className="sesion">
      <aside className="sesion-marca">
        <div className="sesion-marca-contenido">
          <p className="sesion-logo">
            <span aria-hidden="true">🚜</span> Montacargas Obregón.
          </p>
          <p className="sesion-lema">Renta de montacargas y equipo para construcción</p>
          <p className="sesion-resumen">
            Cotiza, sigue cada renta y coordina revisiones y traslados desde un solo lugar.
          </p>
          <ul className="sesion-beneficios">
            {beneficios.map((b) => (
              <li key={b}>
                <span className="material-symbols-rounded" aria-hidden="true">
                  check_circle
                </span>
                {b}
              </li>
            ))}
          </ul>
        </div>
        <p className="sesion-ciudad">Ciudad Obregón, Sonora.</p>
      </aside>

      {/* En tableta y celular la marca va en una barra arriba */}
      <header className="sesion-barra">
        <span aria-hidden="true">🚜</span> Montacargas Obregón.
      </header>

      <main className="sesion-acceso">
        <div className="sesion-tarjeta">
          <div className="sesion-titulos">
            <h1>Inicia sesión</h1>
            <p>Escribe tu correo y tu contraseña. Tu rol (Auxiliar o Gerencia) viene de tu usuario.</p>
          </div>

          <form className="sesion-form" noValidate onSubmit={alEntrar}>
            <AvisoInicio error={error} estado={estado} />

            <div className="sesion-campo">
              <label htmlFor={`${id}-correo`}>Correo</label>
              <div className={"sesion-caja" + (correoFalta ? " con-error" : "")}>
                <span className="material-symbols-rounded" aria-hidden="true">
                  person
                </span>
                <input
                  id={`${id}-correo`}
                  type="email"
                  autoComplete="username"
                  placeholder="tu.correo@mobregon.mx"
                  value={correo}
                  aria-invalid={correoFalta}
                  onChange={(e) => setCorreo(e.target.value)}
                />
              </div>
            </div>

            <div className="sesion-campo">
              <label htmlFor={`${id}-contrasena`}>Contraseña</label>
              <div className={"sesion-caja" + (contrasenaFalta ? " con-error" : "")}>
                <span className="material-symbols-rounded" aria-hidden="true">
                  lock
                </span>
                <input
                  id={`${id}-contrasena`}
                  type={mostrar ? "text" : "password"}
                  autoComplete="current-password"
                  placeholder="Escribe tu contraseña"
                  value={contrasena}
                  aria-invalid={contrasenaFalta}
                  onChange={(e) => setContrasena(e.target.value)}
                />
                <button
                  type="button"
                  className="sesion-ojo"
                  aria-label={mostrar ? "Ocultar contraseña" : "Mostrar contraseña"}
                  aria-pressed={mostrar}
                  onClick={() => setMostrar(!mostrar)}
                >
                  <span className="material-symbols-rounded" aria-hidden="true">
                    {mostrar ? "visibility_off" : "visibility"}
                  </span>
                </button>
              </div>
            </div>

            <Button type="submit" className="sesion-entrar" disabled={enviando}>
              Entrar
              <span className="material-symbols-rounded" aria-hidden="true">
                arrow_forward
              </span>
            </Button>

            <p className="sesion-nota">
              <span className="material-symbols-rounded" aria-hidden="true">
                info
              </span>
              ¿Olvidaste tu contraseña? Pídele una nueva a la oficina.
            </p>
          </form>
        </div>
      </main>
    </div>
  );
}
