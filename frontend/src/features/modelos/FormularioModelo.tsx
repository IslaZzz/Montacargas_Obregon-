import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "../../components/buttons";
import Campo from "../../components/CampoForm";
import { useTitulo } from "../../hooks/useTitulo";
import { ErrorApi } from "../../services/apiClient";
import { listarCategorias, type Categoria } from "../categorias/categoriasApi";
import {
  actualizarModelo,
  camposDeCategoria,
  crearModelo,
  obtenerModelo,
} from "./modelosApi";
import type { CampoCategoria } from "./modelosTypes";
import type { AvisoModelo } from "./Modelos";
import "../../styles/modelos/modelos.css";

const MAXIMO_DESCRIPCION = 120;
const OBLIGATORIO = "Este dato es obligatorio";
const NOMBRE_REPETIDO = "Ya existe un modelo con ese nombre";

// "3000" se muestra como "3,000" en el campo, como en el diseño
const conComas = (valor: string) =>
  /^\d+$/.test(valor) ? Number(valor).toLocaleString("en-US") : valor;

// Etiqueta del campo con su unidad: «Capacidad de carga (kg)»
const etiquetaDe = (c: CampoCategoria) => (c.unidad ? `${c.nombre} (${c.unidad})` : c.nombre);

// Ejemplo que se muestra en el campo vacío
const EJEMPLOS: Record<string, string> = {
  ENTERO: "Ej. 3,000",
  DECIMAL: "Ej. 4.5",
};

export default function FormularioModelo() {
  const { id } = useParams();
  const modeloId = id ? Number(id) : null;
  const editando = modeloId !== null;
  useTitulo(editando ? "Editar modelo" : "Nuevo modelo");
  const navigate = useNavigate();

  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [campos, setCampos] = useState<CampoCategoria[]>([]);
  const [cargando, setCargando] = useState(true);

  const [categoriaId, setCategoriaId] = useState("");
  const [nombre, setNombre] = useState("");
  const [marcaModelo, setMarcaModelo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  // Valor de cada especificación por id del campo
  const [valores, setValores] = useState<Record<number, string>>({});

  // True después de pulsar Guardar: desde entonces se marcan los vacíos
  const [enviado, setEnviado] = useState(false);
  const [erroresServidor, setErroresServidor] = useState<Record<string, string>>({});
  const [mensajeServidor, setMensajeServidor] = useState("");
  const [guardando, setGuardando] = useState(false);

  // Categorías para la lista y, al editar, los datos del modelo
  useEffect(() => {
    Promise.all([listarCategorias(), modeloId ? obtenerModelo(modeloId) : null])
      .then(([listaCategorias, modelo]) => {
        setCategorias(listaCategorias);
        if (modelo) {
          setCategoriaId(String(modelo.categoriaId));
          setNombre(modelo.nombre);
          setMarcaModelo(modelo.marcaModelo);
          setDescripcion(modelo.descripcion ?? "");
          setValores(
            Object.fromEntries(
              modelo.especificaciones.map((e) => [
                e.campoId,
                e.tipo === "ENTERO" ? conComas(e.valor) : e.valor,
              ]),
            ),
          );
        }
      })
      .catch(() => setMensajeServidor("No fue posible cargar los datos del modelo."))
      .finally(() => setCargando(false));
  }, [modeloId]);

  // Los campos de especificación dependen de la categoría elegida
  useEffect(() => {
    if (!categoriaId) return;
    let vigente = true;
    camposDeCategoria(Number(categoriaId))
      .then((lista) => vigente && setCampos(lista))
      .catch(() => vigente && setCampos([]));
    return () => {
      vigente = false;
    };
  }, [categoriaId]);

  function validar() {
    const e: Record<string, string> = {};
    if (!categoriaId) e.categoriaId = OBLIGATORIO;
    if (!nombre.trim()) e.nombre = OBLIGATORIO;
    if (!marcaModelo.trim()) e.marcaModelo = OBLIGATORIO;
    campos.forEach((c) => {
      if (c.requerido && !valores[c.id]?.trim()) e[`especificaciones.${c.id}`] = OBLIGATORIO;
    });
    return e;
  }

  const errores = enviado ? { ...erroresServidor, ...validar() } : erroresServidor;
  const total = Object.keys(errores).length;
  const nombreRepetido = errores.nombre === NOMBRE_REPETIDO;

  // Al corregir un campo se quita el error que el backend marcó en él
  const quitarError = (llave: string) =>
    setErroresServidor((actuales) => {
      if (!(llave in actuales)) return actuales;
      const resto = { ...actuales };
      delete resto[llave];
      return resto;
    });

  const cambiarCategoria = (valor: string) => {
    setCategoriaId(valor);
    // Otra categoría pide otros campos
    setCampos([]);
    setValores({});
    setErroresServidor({});
  };

  const cambiarValor = (campo: CampoCategoria, valor: string) => {
    setValores({ ...valores, [campo.id]: valor });
    quitarError(`especificaciones.${campo.id}`);
  };

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

    const datos = {
      categoriaId: Number(categoriaId),
      nombre: nombre.trim(),
      marcaModelo: marcaModelo.trim(),
      descripcion: descripcion.trim() || null,
      // Solo los campos de la categoría elegida
      especificaciones: Object.fromEntries(
        campos.filter((c) => valores[c.id]?.trim()).map((c) => [c.id, valores[c.id].trim()]),
      ),
    };

    setGuardando(true);
    try {
      if (modeloId) await actualizarModelo(modeloId, datos);
      else await crearModelo(datos);
      const aviso: AvisoModelo = editando ? "actualizado" : "creado";
      navigate("/modelos", { state: { aviso } });
    } catch (err) {
      if (err instanceof ErrorApi && Object.keys(err.errores).length > 0) {
        setErroresServidor(err.errores);
      } else {
        setMensajeServidor(err instanceof Error ? err.message : "No fue posible guardar el modelo.");
      }
      setGuardando(false);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  }

  const campoEspecificacion = (c: CampoCategoria) => {
    const llave = `especificaciones.${c.id}`;
    const valor = valores[c.id] ?? "";
    const error = errores[llave];

    if (c.tipo === "LISTA" || c.tipo === "BOOLEANO") {
      const opciones = c.tipo === "LISTA" ? c.opciones : ["Sí", "No"];
      return (
        <label className="campo" key={c.id}>
          <span className="campo-etiqueta">
            {etiquetaDe(c)} <small>{c.requerido ? "(obligatorio)" : "(opcional)"}</small>
          </span>
          <select
            className={"campo-input" + (error ? " con-error" : "")}
            aria-invalid={error ? true : undefined}
            value={valor}
            onChange={(e) => cambiarValor(c, e.target.value)}
          >
            <option value="">Elige una opción</option>
            {opciones.map((o) => (
              <option key={o} value={o}>
                {o}
              </option>
            ))}
          </select>
          {error && (
            <span className="campo-error" role="alert">
              <span className="material-symbols-rounded">error</span>
              {error}
            </span>
          )}
        </label>
      );
    }

    return (
      <Campo
        key={c.id}
        etiqueta={etiquetaDe(c)}
        obligatorio={c.requerido}
        opcional={!c.requerido}
        type={c.tipo === "FECHA" ? "date" : "text"}
        inputMode={c.tipo === "ENTERO" || c.tipo === "DECIMAL" ? "decimal" : undefined}
        placeholder={EJEMPLOS[c.tipo]}
        value={valor}
        error={error}
        onChange={(v) => cambiarValor(c, v)}
        onBlur={() => c.tipo === "ENTERO" && cambiarValor(c, conComas(valor.replace(/,/g, "")))}
      />
    );
  };

  return (
    <form className="pagina-form modelo-form" onSubmit={guardar} noValidate>
      <div className="pagina">
        <h1>{editando ? "Editar modelo" : "Nuevo modelo"}</h1>
        <p>
          {editando
            ? "Cambia los datos del modelo. Las unidades de este modelo mostrarán los datos nuevos."
            : "Captura los datos del modelo. Todas las unidades de este modelo comparten sus datos y su tarifa."}
        </p>

        {(total > 0 || mensajeServidor) && (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>
                {nombreRepetido
                  ? "No se pudo guardar: ese nombre ya existe"
                  : total > 0
                    ? `No se pudo guardar: hay ${total} ${total === 1 ? "dato" : "datos"} por corregir`
                    : "No se pudo guardar"}
              </strong>
              <p>
                {nombreRepetido
                  ? 'Ya hay un modelo con ese nombre. Cámbialo y vuelve a presionar "Guardar".'
                  : total > 0
                    ? 'Revisa los campos marcados en rojo y vuelve a presionar "Guardar".'
                    : mensajeServidor}
              </p>
            </div>
          </div>
        )}

        <div className="secciones">
          <div className="modelo-columna">
            <section className="tarjeta">
              <h2>
                <span className="paso">1</span> Categoría
              </h2>
              <label className="campo">
                <span className="campo-etiqueta">
                  Categoría del equipo <small>(obligatorio)</small>
                </span>
                <select
                  className={"campo-input" + (errores.categoriaId ? " con-error" : "")}
                  aria-invalid={errores.categoriaId ? true : undefined}
                  value={categoriaId}
                  disabled={cargando}
                  onChange={(e) => cambiarCategoria(e.target.value)}
                >
                  <option value="">{cargando ? "Cargando categorías…" : "Elige una categoría"}</option>
                  {categorias.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nombre}
                    </option>
                  ))}
                </select>
                {errores.categoriaId && (
                  <span className="campo-error" role="alert">
                    <span className="material-symbols-rounded">error</span>
                    {errores.categoriaId}
                  </span>
                )}
              </label>
            </section>

            <section className="tarjeta">
              <h2>
                <span className="paso">2</span> Datos del modelo
              </h2>
              <Campo
                etiqueta="Nombre"
                obligatorio
                maxLength={100}
                placeholder="Ej. Montacargas eléctrico 3 t"
                value={nombre}
                error={errores.nombre}
                onChange={(v) => {
                  setNombre(v);
                  quitarError("nombre");
                }}
              />
              <Campo
                etiqueta="Marca y modelo"
                obligatorio
                maxLength={150}
                placeholder="Ej. Toyota 8FBE30"
                value={marcaModelo}
                error={errores.marcaModelo}
                onChange={(v) => {
                  setMarcaModelo(v);
                  quitarError("marcaModelo");
                }}
              />
              <div className="campo">
                <Campo
                  etiqueta="Descripción"
                  opcional
                  maxLength={MAXIMO_DESCRIPCION}
                  placeholder="Ej. Montacargas eléctrico para uso en interiores."
                  value={descripcion}
                  error={errores.descripcion}
                  onChange={(v) => {
                    setDescripcion(v);
                    quitarError("descripcion");
                  }}
                />
                <span className="modelo-ayuda">Máximo {MAXIMO_DESCRIPCION} caracteres.</span>
              </div>
            </section>
          </div>

          {campos.length > 0 && (
            <div className="modelo-columna">
              <section className="tarjeta">
                <h2>
                  <span className="paso">3</span> Especificaciones
                </h2>
                {campos.map(campoEspecificacion)}
              </section>
            </div>
          )}
        </div>
      </div>

      <div className="barra-acciones">
        <p>Los datos se guardan solo al presionar "Guardar".</p>
        <Button type="button" variant="secondary" onClick={() => navigate("/modelos")}>
          Cancelar
        </Button>
        <Button type="submit" variant="primary" disabled={guardando || cargando}>
          Guardar
        </Button>
      </div>
    </form>
  );
}
