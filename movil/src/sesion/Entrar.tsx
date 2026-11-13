import { useState } from "react";
import type { FormEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import BarraSuperior from "../components/BarraSuperior";
import Boton from "../components/Boton";
import Icono from "../components/Icono";
import Pantalla from "../components/Pantalla";
import { mensajeDeError } from "../services/api";
import { INICIO } from "./navegacion";
import { entrarConCodigo } from "./sesionApi";
import { useSesion } from "./useSesion";

/**
 * Entrar con el código (MEC-10, OP-01…).
 */
export default function Entrar() {
  const { personal: actual, entrar } = useSesion();
  const navigate = useNavigate();
  const desde = (useLocation().state as { desde?: string } | null)?.desde;
  const [codigo, setCodigo] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const limpio = codigo.trim();
    if (!limpio) {
      setError("Escribe tu código.");
      return;
    }

    setEnviando(true);
    setError(null);
    try {
      const persona = await entrarConCodigo(limpio);
      entrar(persona);
      navigate(desde ?? INICIO[persona.tipo], { replace: true });
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo entrar."));
      setEnviando(false);
    }
  }

  return (
    <Pantalla barraSuperior={<BarraSuperior atras={actual ? INICIO[actual.tipo] : undefined} />}>
      <form className="entrar" onSubmit={enviar}>
        <Icono nombre="badge" className="entrar-icono" />
        <h2>Ingresa tu código</h2>
        <p className="entrar-ayuda">Por ejemplo MEC-10.</p>

        <input
          className={`entrar-campo${error ? " entrar-campo-error" : ""}`}
          value={codigo}
          onChange={(e) => {
            setCodigo(e.target.value.toUpperCase());
            setError(null);
          }}
          placeholder="MEC-10"
          aria-label="Código"
          aria-invalid={Boolean(error)}
          autoFocus
          autoComplete="off"
          autoCapitalize="characters"
          autoCorrect="off"
          spellCheck={false}
          maxLength={20}
          enterKeyHint="go"
        />

        {error && (
          <p className="entrar-error" role="alert">
            <Icono nombre="error" relleno />
            {error}
          </p>
        )}

        <Boton type="submit" icono="arrow_forward" disabled={enviando}>
          {enviando ? "Entrando…" : "Entrar"}
        </Boton>
      </form>

      {actual && (
        <p className="entrar-actual">
          Sigues como <strong>{actual.nombre}</strong>.{" "}
          <button type="button" className="entrar-enlace" onClick={() => navigate(INICIO[actual.tipo], { replace: true })}>
            Continuar
          </button>
        </p>
      )}
    </Pantalla>
  );
}