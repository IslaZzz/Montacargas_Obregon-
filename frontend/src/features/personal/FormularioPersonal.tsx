import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../components/buttons";
import Campo from "../../components/CampoForm";
import { ErrorApi } from "../../services/apiClient";
import type { DatosPersonal, Personal, TipoPersonal } from "./personalTypes";
import { ICONO_TIPO, NOMBRE_TIPO } from "./personalTypes";
import "../../styles/personal/personal.css";

type CampoPersonal = "codigo" | "nombres" | "apellidoPaterno" | "apellidoMaterno" | "tipo";

interface FormularioPersonalProps {
  titulo: string;
  descripcion: ReactNode;
  textoGuardar: string; // "Registrar personal" o "Guardar cambios"
  inicial?: Personal; // Al editar, los datos actuales
  avisoCodigo?: ReactNode; // Nota junto al código
  onGuardar: (datos: DatosPersonal) => Promise<void>;
}

// Lo mismo que acepta el backend (PersonalRequest)
const CODIGO_VALIDO = /^[A-Z0-9-]+$/;
const NOMBRE_VALIDO = /^[\p{L} .'-]+$/u;

// El código va en mayúsculas y sin espacios, como lo guarda el backend
const limpiarCodigo = (valor: string) => valor.toUpperCase().replace(/\s/g, "").slice(0, 20);

function validar(d: Record<Exclude<CampoPersonal, "tipo">, string> & { tipo: TipoPersonal | "" }) {
  const e: Partial<Record<CampoPersonal, string>> = {};
  if (!d.codigo.trim()) e.codigo = "Escribe el código.";
  else if (!CODIGO_VALIDO.test(d.codigo)) e.codigo = "El código solo admite letras, dígitos y guiones.";

  if (!d.nombres.trim()) e.nombres = "Escribe el nombre.";
  else if (!NOMBRE_VALIDO.test(d.nombres.trim())) e.nombres = "El nombre solo admite letras.";

  if (!d.apellidoPaterno.trim()) e.apellidoPaterno = "Escribe el apellido paterno.";
  else if (!NOMBRE_VALIDO.test(d.apellidoPaterno.trim())) e.apellidoPaterno = "El apellido paterno solo admite letras.";

  if (d.apellidoMaterno.trim() && !NOMBRE_VALIDO.test(d.apellidoMaterno.trim())) {
    e.apellidoMaterno = "El apellido materno solo admite letras.";
  }
  if (!d.tipo) e.tipo = "Elige si es operador de traslado o mecánico.";
  return e;
}

/** Formulario de registrar (OHU-26) y editar (OHU-28) personal. */
export default function FormularioPersonal({
  titulo,
  descripcion,
  textoGuardar,
  inicial,
  avisoCodigo,
  onGuardar,
}: FormularioPersonalProps) {
  const navigate = useNavigate();
  const [datos, setDatos] = useState({
    codigo: inicial?.codigo ?? "",
    nombres: inicial?.nombres ?? "",
    apellidoPaterno: inicial?.apellidoPaterno ?? "",
    apellidoMaterno: inicial?.apellidoMaterno ?? "",
    tipo: (inicial?.tipo ?? "") as TipoPersonal | "",
  });
  const [enviado, setEnviado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [erroresServidor, setErroresServidor] = useState<Partial<Record<CampoPersonal, string>>>({});
  const [mensajeServidor, setMensajeServidor] = useState("");

  // Los errores se muestran después de intentar guardar
  const errores = enviado ? { ...erroresServidor, ...validar(datos) } : {};
  const total = Object.keys(errores).length;

  const cambiar = (campo: CampoPersonal, valor: string) => {
    setDatos((d) => ({ ...d, [campo]: valor }));
    setErroresServidor((e) => {
      if (!(campo in e)) return e;
      const resto = { ...e };
      delete resto[campo];
      return resto;
    });
  };

  async function guardar(e: FormEvent) {
    e.preventDefault();
    setEnviado(true);
    setErroresServidor({});
    setMensajeServidor("");
    if (Object.keys(validar(datos)).length > 0 || !datos.tipo) return;

    setGuardando(true);
    try {
      await onGuardar({
        codigo: datos.codigo.trim(),
        nombres: datos.nombres.trim(),
        apellidoPaterno: datos.apellidoPaterno.trim(),
        apellidoMaterno: datos.apellidoMaterno.trim() || null,
        tipo: datos.tipo,
      });
    } catch (err) {
      // Los errores de un campo van debajo de él; "Ya existe personal con ese código" también va en el código
      const porCampo: Partial<Record<CampoPersonal, string>> = {};
      if (err instanceof ErrorApi) {
        for (const [campo, texto] of Object.entries(err.errores)) {
          if (campo in datos) porCampo[campo as CampoPersonal] = texto;
        }
        if (Object.keys(porCampo).length === 0 && /código/i.test(err.message)) porCampo.codigo = err.message;
      }
      setErroresServidor(porCampo);
      if (Object.keys(porCampo).length === 0) {
        setMensajeServidor(err instanceof Error ? err.message : "No fue posible guardar.");
      }
      window.scrollTo({ top: 0, behavior: "smooth" });
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="pagina-form" onSubmit={guardar} noValidate>
      <div className="pagina formulario-personal">
        <button type="button" className="personal-volver" onClick={() => navigate("/personal")}>
          <span className="material-symbols-rounded" aria-hidden="true">
            arrow_back
          </span>
          Volver a personal
        </button>
        <h1>{titulo}</h1>
        <p>{descripcion}</p>

        {(total > 0 || mensajeServidor) && (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>
                {total > 0
                  ? `No se pudo guardar: hay ${total} ${total === 1 ? "dato" : "datos"} por corregir`
                  : "No se pudo guardar"}
              </strong>
              <p>{total > 0 ? "Revisa los campos marcados en rojo." : mensajeServidor}</p>
            </div>
          </div>
        )}

        <section className="tarjeta personal-tarjeta">
          <fieldset className="personal-tipos" aria-invalid={errores.tipo ? true : undefined}>
            <legend className="campo-etiqueta">
              Puesto <small>(obligatorio)</small>
            </legend>
            <div className="personal-tipos-opciones">
              {(["OP_TRASLADO", "MECANICO"] as const).map((t) => (
                <label key={t} className={`personal-tipo${datos.tipo === t ? " personal-tipo-elegido" : ""}`}>
                  <input
                    type="radio"
                    name="tipo"
                    value={t}
                    checked={datos.tipo === t}
                    onChange={() => cambiar("tipo", t)}
                  />
                  <span className="material-symbols-rounded" aria-hidden="true">
                    {ICONO_TIPO[t]}
                  </span>
                  {NOMBRE_TIPO[t]}
                </label>
              ))}
            </div>
            {errores.tipo && (
              <span className="campo-error" role="alert">
                <span className="material-symbols-rounded">error</span>
                {errores.tipo}
              </span>
            )}
          </fieldset>

          <div className="personal-campos">
            <Campo
              etiqueta="Nombre(s)"
              obligatorio
              maxLength={50}
              autoComplete="off"
              value={datos.nombres}
              error={errores.nombres}
              onChange={(v) => cambiar("nombres", v)}
            />
            <Campo
              etiqueta="Apellido paterno"
              obligatorio
              maxLength={50}
              autoComplete="off"
              value={datos.apellidoPaterno}
              error={errores.apellidoPaterno}
              onChange={(v) => cambiar("apellidoPaterno", v)}
            />
            <Campo
              etiqueta="Apellido materno"
              maxLength={50}
              autoComplete="off"
              value={datos.apellidoMaterno}
              error={errores.apellidoMaterno}
              onChange={(v) => cambiar("apellidoMaterno", v)}
            />
            <Campo
              etiqueta="Código"
              obligatorio
              maxLength={20}
              autoCapitalize="characters"
              autoComplete="off"
              placeholder="Ej. OP-014"
              value={datos.codigo}
              error={errores.codigo}
              onChange={(v) => cambiar("codigo", limpiarCodigo(v))}
            />
          </div>

          <p className="nota">
            <span className="material-symbols-rounded">badge</span>
            {avisoCodigo ?? "Es el código del gafete: con él entra a la app móvil. No se puede repetir."}
          </p>
        </section>
      </div>

      <div className="barra-acciones">
        <p>Los datos se guardan solo al presionar "{textoGuardar}".</p>
        <Button type="button" variant="secondary" onClick={() => navigate("/personal")}>
          Cancelar
        </Button>
        <Button type="submit" variant="primary" disabled={guardando}>
          {guardando ? "Guardando…" : textoGuardar}
        </Button>
      </div>
    </form>
  );
}
