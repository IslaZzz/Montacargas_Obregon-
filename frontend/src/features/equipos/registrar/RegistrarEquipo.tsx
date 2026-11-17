import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import Aviso from "../../../components/Aviso";
import { Button } from "../../../components/buttons";
import Campo from "../../../components/CampoForm";
import { useTitulo } from "../../../hooks/useTitulo";
import { ErrorApi } from "../../../services/apiClient";
import { listarModelos } from "../../modelos/modelosApi";
import type { Modelo } from "../../modelos/modelosTypes";
import { crearEtiqueta, listarEtiquetas, registrarEquipo } from "./equiposApi";
import type { Etiqueta } from "./equiposApi";
import "../../../styles/clientes/registrar-cliente.css";
import "../../../styles/filters.css";
import "../../../styles/equipos/registrar-equipo.css";

const OBLIGATORIO = "Este dato es obligatorio";
const ANIO_MINIMO = 1950;
const ANIO_ACTUAL = new Date().getFullYear();
const ANIO_INVALIDO = `Escribe un año de 4 dígitos, entre ${ANIO_MINIMO} y ${ANIO_ACTUAL}`;
const SOLO_LETRAS_DIGITOS = "Solo admite letras, dígitos y guiones";
const FORMATO_NUMERO = /^[A-Za-z0-9-]+$/;

// "3000" se muestra como "3,000 kg"
function capacidadDe(modelo: Modelo) {
  const capacidad = modelo.especificaciones.find((e) => e.nombre === "Capacidad de carga");
  if (!capacidad) return "—";
  const valor = /^\d+$/.test(capacidad.valor)
    ? Number(capacidad.valor).toLocaleString("en-US")
    : capacidad.valor;
  return capacidad.unidad ? `${valor} ${capacidad.unidad}` : valor;
}

// Formulario «Registrar equipo» (HU-02). Estado, ubicación y horas de uso los maneja Operativo.
export default function RegistrarEquipo() {
  useTitulo("Registrar equipo");
  const navigate = useNavigate();

  const [modelos, setModelos] = useState<Modelo[]>([]);
  const [etiquetas, setEtiquetas] = useState<Etiqueta[]>([]);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState(false);

  const [modeloId, setModeloId] = useState("");
  const [numeroUnidad, setNumeroUnidad] = useState("");
  const [numeroSerie, setNumeroSerie] = useState("");
  const [anioCompra, setAnioCompra] = useState("");
  const [elegidas, setElegidas] = useState<number[]>([]);

  // «+ Nueva etiqueta»: null mientras no se está capturando una
  const [nuevaEtiqueta, setNuevaEtiqueta] = useState<string | null>(null);
  const [errorEtiqueta, setErrorEtiqueta] = useState("");
  const [guardandoEtiqueta, setGuardandoEtiqueta] = useState(false);

  // True después de pulsar Guardar: desde entonces se marcan los vacíos
  const [enviado, setEnviado] = useState(false);
  const [erroresServidor, setErroresServidor] = useState<Record<string, string>>({});
  const [mensajeServidor, setMensajeServidor] = useState("");
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    Promise.all([listarModelos(), listarEtiquetas()])
      .then(([listaModelos, listaEtiquetas]) => {
        setModelos(listaModelos);
        setEtiquetas(listaEtiquetas);
      })
      .catch(() => setErrorCarga(true))
      .finally(() => setCargando(false));
  }, []);

  const modelo = modelos.find((m) => String(m.id) === modeloId);
  const sinModelos = !cargando && !errorCarga && modelos.length === 0;

  function validar() {
    const e: Record<string, string> = {};
    if (!modeloId) e.modeloId = OBLIGATORIO;
    if (!numeroUnidad.trim()) e.numeroUnidad = OBLIGATORIO;
    else if (!FORMATO_NUMERO.test(numeroUnidad.trim())) e.numeroUnidad = SOLO_LETRAS_DIGITOS;
    if (!numeroSerie.trim()) e.numeroSerie = OBLIGATORIO;
    else if (!FORMATO_NUMERO.test(numeroSerie.trim())) e.numeroSerie = SOLO_LETRAS_DIGITOS;
    const anio = Number(anioCompra);
    if (anioCompra && (!/^\d{4}$/.test(anioCompra) || anio < ANIO_MINIMO || anio > ANIO_ACTUAL)) {
      e.anioCompra = ANIO_INVALIDO;
    }
    return e;
  }

  const errores = enviado ? { ...erroresServidor, ...validar() } : erroresServidor;
  const total = Object.keys(errores).length;
  // Número de unidad o de serie repetido: el backend dice cuál
  const repetido = Object.values(erroresServidor).find((m) => m.startsWith("Ya existe un equipo"));

  // Al corregir un campo se quita el error que el backend marcó en él
  const quitarError = (llave: string) =>
    setErroresServidor((actuales) => {
      if (!(llave in actuales)) return actuales;
      const resto = { ...actuales };
      delete resto[llave];
      return resto;
    });

  const alternarEtiqueta = (id: number) =>
    setElegidas((actuales) =>
      actuales.includes(id) ? actuales.filter((x) => x !== id) : [...actuales, id],
    );

  // Guarda la etiqueta nueva y la deja elegida
  async function agregarEtiqueta() {
    const nombre = (nuevaEtiqueta ?? "").trim();
    if (!nombre) {
      setErrorEtiqueta(OBLIGATORIO);
      return;
    }
    setGuardandoEtiqueta(true);
    try {
      const creada = await crearEtiqueta(nombre);
      setEtiquetas((actuales) =>
        [...actuales, creada].sort((a, b) => a.nombre.localeCompare(b.nombre, "es")),
      );
      setElegidas((actuales) => [...actuales, creada.id]);
      setNuevaEtiqueta(null);
      setErrorEtiqueta("");
    } catch (err) {
      setErrorEtiqueta(
        err instanceof ErrorApi
          ? (err.errores.nombre ?? err.message)
          : "No fue posible guardar la etiqueta.",
      );
    } finally {
      setGuardandoEtiqueta(false);
    }
  }

  async function guardar(e: FormEvent) {
    e.preventDefault();
    if (guardando) return;
    setEnviado(true);
    setErroresServidor({});
    setMensajeServidor("");

    if (Object.keys(validar()).length > 0) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    setGuardando(true);
    try {
      await registrarEquipo({
        modeloId: Number(modeloId),
        numeroUnidad: numeroUnidad.trim(),
        numeroSerie: numeroSerie.trim(),
        anioCompra: anioCompra.trim() ? Number(anioCompra.trim()) : null,
        etiquetaIds: elegidas,
      });
      navigate("/equipos", { state: { aviso: "registrado" } });
    } catch (err) {
      if (err instanceof ErrorApi && Object.keys(err.errores).length > 0) {
        setErroresServidor(err.errores);
      } else {
        setMensajeServidor(err instanceof Error ? err.message : "No fue posible guardar el equipo.");
      }
      setGuardando(false);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  }

  if (sinModelos) {
    return (
      <div className="pagina">
        <h1>Registrar equipo</h1>
        <div className="equipo-sin-modelos" role="status">
          <span className="material-symbols-rounded" aria-hidden="true">
            sell
          </span>
          <strong>Primero registra un modelo</strong>
          <p>Cada equipo pertenece a un modelo. Regístralo y vuelve a esta pantalla.</p>
          <Link className="btn btn-primary" to="/modelos">
            Ir a «Modelos»
          </Link>
        </div>
      </div>
    );
  }

  return (
    <form className="pagina-form equipo-form" onSubmit={guardar} noValidate>
      <div className="pagina">
        <h1>Registrar equipo</h1>
        <p>
          Captura los datos de la unidad. Quedará «En revisión de entrada» hasta que Operativo la
          ponga disponible.
        </p>

        {errorCarga && (
          <Aviso tipo="error" titulo="No fue posible cargar los modelos y las etiquetas">
            Revisa tu conexión y recarga la página para intentarlo de nuevo.
          </Aviso>
        )}

        {(total > 0 || mensajeServidor) && (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>
                {repetido
                  ? "No se pudo guardar: ese número ya existe"
                  : total > 0
                    ? `No se pudo guardar: hay ${total} ${total === 1 ? "dato" : "datos"} por corregir`
                    : "No se pudo guardar"}
              </strong>
              <p>
                {repetido
                  ? `${repetido}. Cámbialo y vuelve a presionar "Guardar".`
                  : total > 0
                    ? 'Revisa los campos marcados en rojo y vuelve a presionar "Guardar".'
                    : mensajeServidor}
              </p>
            </div>
          </div>
        )}

        <div className="secciones">
          <section className="tarjeta">
            <h2>
              <span className="paso">1</span> Modelo
            </h2>
            <label className="campo">
              <span className="campo-etiqueta">
                Modelo <small>(obligatorio)</small>
              </span>
              <select
                className={"campo-input" + (errores.modeloId ? " con-error" : "")}
                aria-invalid={errores.modeloId ? true : undefined}
                value={modeloId}
                disabled={cargando}
                onChange={(e) => {
                  setModeloId(e.target.value);
                  quitarError("modeloId");
                }}
              >
                <option value="">{cargando ? "Cargando modelos…" : "Elige un modelo"}</option>
                {modelos.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.nombre}
                  </option>
                ))}
              </select>
              {errores.modeloId && (
                <span className="campo-error" role="alert">
                  <span className="material-symbols-rounded">error</span>
                  {errores.modeloId}
                </span>
              )}
            </label>

            {/* Datos del modelo elegido; no se editan aquí */}
            {modelo && (
              <dl className="equipo-modelo">
                <div>
                  <dt>Categoría</dt>
                  <dd>{modelo.categoria}</dd>
                </div>
                <div>
                  <dt>Marca y modelo</dt>
                  <dd>{modelo.marcaModelo}</dd>
                </div>
                <div>
                  <dt>Capacidad</dt>
                  <dd>{capacidadDe(modelo)}</dd>
                </div>
                <div className="equipo-modelo-ancho">
                  <dt>Descripción</dt>
                  <dd>{modelo.descripcion || "—"}</dd>
                </div>
              </dl>
            )}
          </section>

          <section className="tarjeta">
            <h2>
              <span className="paso">2</span> Datos del equipo
            </h2>
            <Campo
              etiqueta="Número de unidad"
              obligatorio
              maxLength={50}
              placeholder="Ej. MC-20"
              value={numeroUnidad}
              error={errores.numeroUnidad}
              onChange={(v) => {
                setNumeroUnidad(v);
                quitarError("numeroUnidad");
              }}
            />
            <Campo
              etiqueta="Número de serie"
              obligatorio
              maxLength={50}
              placeholder="Ej. TY8FBE-000020"
              value={numeroSerie}
              error={errores.numeroSerie}
              onChange={(v) => {
                setNumeroSerie(v);
                quitarError("numeroSerie");
              }}
            />
            <Campo
              etiqueta="Año de compra o fabricación"
              opcional
              inputMode="numeric"
              maxLength={4}
              placeholder="Ej. 2024"
              value={anioCompra}
              error={errores.anioCompra}
              onChange={(v) => {
                setAnioCompra(v.replace(/\D/g, ""));
                quitarError("anioCompra");
              }}
            />

            <div className="campo">
              <span className="campo-etiqueta" id="equipo-etiquetas">
                Etiquetas <small>(opcional)</small>
              </span>
              <div className="filters-row" role="group" aria-labelledby="equipo-etiquetas">
                {etiquetas.map((t) => (
                  <button
                    type="button"
                    key={t.id}
                    className={elegidas.includes(t.id) ? "filter-chip active" : "filter-chip"}
                    aria-pressed={elegidas.includes(t.id)}
                    onClick={() => alternarEtiqueta(t.id)}
                  >
                    {t.nombre}
                  </button>
                ))}
                {nuevaEtiqueta === null && (
                  <button
                    type="button"
                    className="filter-chip equipo-nueva-etiqueta"
                    onClick={() => setNuevaEtiqueta("")}
                  >
                    + Nueva etiqueta
                  </button>
                )}
              </div>

              {nuevaEtiqueta !== null && (
                <div className="equipo-etiqueta-nueva">
                  <Campo
                    etiqueta="Nombre de la etiqueta"
                    autoFocus
                    maxLength={50}
                    placeholder="Ej. Llanta sólida"
                    value={nuevaEtiqueta}
                    error={errorEtiqueta}
                    onChange={(v) => {
                      setNuevaEtiqueta(v);
                      setErrorEtiqueta("");
                    }}
                    onKeyDown={(e) => {
                      // Enter agrega la etiqueta; no debe guardar el equipo
                      if (e.key === "Enter") {
                        e.preventDefault();
                        agregarEtiqueta();
                      }
                    }}
                  />
                  <div className="equipo-etiqueta-botones">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => {
                        setNuevaEtiqueta(null);
                        setErrorEtiqueta("");
                      }}
                    >
                      Cancelar
                    </Button>
                    <Button
                      type="button"
                      variant="primary"
                      disabled={guardandoEtiqueta}
                      onClick={agregarEtiqueta}
                    >
                      Agregar
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </section>
        </div>
      </div>

      <div className="barra-acciones">
        <p>Los datos se guardan solo al presionar "Guardar".</p>
        <Button type="button" variant="secondary" onClick={() => navigate("/equipos")}>
          Cancelar
        </Button>
        <Button type="submit" variant="primary" disabled={guardando || cargando || errorCarga}>
          Guardar
        </Button>
      </div>
    </form>
  );
}
