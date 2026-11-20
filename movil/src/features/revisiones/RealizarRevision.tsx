import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import BarraAccion from "../../components/BarraAccion";
import BarraSuperior from "../../components/BarraSuperior";
import Boton from "../../components/Boton";
import Cargando from "../../components/Cargando";
import Chip from "../../components/Chip";
import { ListaDatos } from "../../components/Datos";
import Encabezado from "../../components/Encabezado";
import Especificaciones from "../../components/Especificaciones";
import Icono from "../../components/Icono";
import { conNotificacion } from "../../components/Notificacion";
import Pantalla from "../../components/Pantalla";
import Resultado from "../../components/Resultado";
import { Tarjeta } from "../../components/Tarjeta";
import CampoFotos from "../../components/formulario/CampoFotos";
import CampoNota from "../../components/formulario/CampoNota";
import CampoOpciones from "../../components/formulario/CampoOpciones";
import CampoSegmentado from "../../components/formulario/CampoSegmentado";
import CampoTexto from "../../components/formulario/CampoTexto";
import { mensajeDeError } from "../../services/api";
import { listarFotos, subirFoto } from "../../services/fotosApi";
import type { Foto } from "../../services/fotosApi";
import { leerNumero } from "../../utils/numeros";
import { iniciarRevision, terminarRevision } from "./revisionesApi";
import type { CampoRevision, DetalleRevision, EstadoInspeccion, RevisionTerminada } from "./revisionesTypes";
import { NOMBRE_TIPO_REVISION, TONO_TIPO_REVISION } from "./revisionesTypes";

// Lo que se lleva escrito en cada campo, por id
type Captura = { valor: string; estado: EstadoInspeccion | null; detalle: string };
const VACIA: Captura = { valor: "", estado: null, detalle: "" };

const OPCIONES_INSPECCION = [
  { valor: "BIEN" as EstadoInspeccion, etiqueta: "Bien" },
  { valor: "ATENCION" as EstadoInspeccion, etiqueta: "Atención", tono: "atencion" as const, pideDetalle: true },
  { valor: "FALLA" as EstadoInspeccion, etiqueta: "Falla", tono: "falla" as const, pideDetalle: true },
];

/**
 * OHU07: la revisión de un equipo, de salida o de regreso.
 * El formulario se arma con los campos de revisión de la categoría del equipo.
 */
export default function RealizarRevision() {
  const id = Number(useParams().id);
  const navigate = useNavigate();

  const [revision, setRevision] = useState<DetalleRevision | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [capturas, setCapturas] = useState<Record<number, Captura>>({});
  const [fotos, setFotos] = useState<Record<string, Foto>>({});
  const [notas, setNotas] = useState("");

  const [intentado, setIntentado] = useState(false); // Los errores se muestran después del primer intento
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const [terminada, setTerminada] = useState<RevisionTerminada | null>(null);

  // Al abrirla queda en proceso; las fotos que ya se habían subido se recuperan
  useEffect(() => {
    iniciarRevision(id)
      .then((detalle) => {
        setRevision(detalle);
        return listarFotos("REVISION", id);
      })
      .then((subidas) => setFotos(Object.fromEntries(subidas.map((f) => [f.etiqueta, f]))))
      .catch((e) => setError(mensajeDeError(e, "No se pudo abrir la revisión.")));
  }, [id]);

  if (terminada) {
    return <Cierre terminada={terminada} onVolver={() => navigate("/revisiones", conNotificacion(`${terminada.unidad} pasó al historial`))} />;
  }

  if (error || !revision) {
    return (
      <Pantalla barraSuperior={<BarraSuperior atras="/revisiones" titulo="Revisión" />}>
        {error ? <Aviso tipo="error" titulo={error} /> : <Cargando />}
      </Pantalla>
    );
  }

  const campos = revision.campos.filter((c) => c.tipo !== "FOTO");
  const fotosObligatorias = revision.campos.filter((c) => c.tipo === "FOTO" && c.requerido).map((c) => c.nombre);
  const fotosOpcionales = revision.campos.filter((c) => c.tipo === "FOTO" && !c.requerido).map((c) => c.nombre);

  // ---- qué falta ----
  const errorDe = (campo: CampoRevision) => errorDeCampo(campo, capturas[campo.id] ?? VACIA);
  const camposFaltantes = campos.filter((c) => errorDe(c)).length;
  const fotosFaltantes = fotosObligatorias.filter((f) => !fotos[f]).length;
  const fallas = campos.filter((c) => c.tipo === "INSPECCION" && capturas[c.id]?.estado === "FALLA").length;
  const completo = camposFaltantes === 0 && fotosFaltantes === 0;
  const totalPasos = Math.max(1, campos.length + fotosObligatorias.length);

  function cambiar(campoId: number, cambio: Partial<Captura>) {
    setCapturas((actual) => ({ ...actual, [campoId]: { ...(actual[campoId] ?? VACIA), ...cambio } }));
  }

  async function subir(etiqueta: string, archivo: File) {
    const foto = await subirFoto("REVISION", id, etiqueta, archivo);
    setFotos((actual) => ({ ...actual, [etiqueta]: foto }));
  }

  async function terminar() {
    if (!revision || !completo) {
      setIntentado(true);
      // Lleva al primer campo con error
      requestAnimationFrame(() => document.querySelector(".pregunta-error")?.scrollIntoView({ behavior: "smooth", block: "center" }));
      return;
    }
    setEnviando(true);
    setFallo(null);
    try {
      const resultado = await terminarRevision(id, {
        valores: campos.map((c) => {
          const captura = capturas[c.id] ?? VACIA;
          return c.tipo === "INSPECCION"
            ? {
                campoId: c.id,
                valor: null,
                estado: captura.estado,
                detalle: captura.estado && captura.estado !== "BIEN" ? captura.detalle.trim() : null,
              }
            : { campoId: c.id, valor: valorParaEnviar(c, captura.valor), estado: null, detalle: null };
        }),
        notas: notas.trim() || null,
      });
      setTerminada(resultado);
    } catch (e) {
      setFallo(mensajeDeError(e, "No se pudo terminar la revisión."));
      setEnviando(false);
    }
  }

  const salida = revision.tipo === "SALIDA";

  return (
    <Pantalla
      barraSuperior={
        <BarraSuperior
          atras="/revisiones"
          titulo={revision.unidad}
          chip={<Chip tono={TONO_TIPO_REVISION[revision.tipo]}>{NOMBRE_TIPO_REVISION[revision.tipo]}</Chip>}
        />
      }
      barraAccion={
        <BarraAccion
          progreso={
            fallas > 0 && completo
              ? { texto: fallas === 1 ? "1 falla" : `${fallas} fallas`, valor: 1, error: true }
              : completo
                ? { texto: "Completo", valor: 1, completo: true }
                : {
                    texto: faltantes(camposFaltantes, fotosFaltantes),
                    valor: (totalPasos - camposFaltantes - fotosFaltantes) / totalPasos,
                    error: intentado,
                  }
          }
        >
          {fallo && <Aviso tipo="error" titulo={fallo} />}
          {fallas > 0 ? (
            <Boton variante="peligro" icono="report" cargando={enviando} onClick={terminar}>
              Reportar falla
            </Boton>
          ) : (
            <Boton icono="check" cargando={enviando} onClick={terminar}>
              {salida ? "Listo para entrega" : "Terminar revisión"}
            </Boton>
          )}
        </BarraAccion>
      }
    >
      <Encabezado titulo={salida ? "Revisión de salida" : "Revisión de regreso"}>
        <Icono nombre="forklift" />
        <span>{[revision.tipoEquipo, revision.modelo, `Renta ${revision.rentaFolio}`].filter(Boolean).join(" · ")}</span>
      </Encabezado>

      {revision.especificaciones.length > 0 && (
        <Tarjeta>
          <h3>Ficha del modelo</h3>
          <ListaDatos>
            <Especificaciones especificaciones={revision.especificaciones} />
          </ListaDatos>
        </Tarjeta>
      )}

      {revision.campos.length === 0 && (
        <Aviso tipo="info" titulo="Esta categoría no tiene campos de revisión">
          Anota en las notas lo que encontraste.
        </Aviso>
      )}

      {campos.map((c) => (
        <CampoDeRevision
          key={c.id}
          campo={c}
          captura={capturas[c.id] ?? VACIA}
          onCambio={(cambio) => cambiar(c.id, cambio)}
          error={intentado ? (errorDe(c) ?? undefined) : undefined}
        />
      ))}

      {fotosObligatorias.length > 0 && (
        <CampoFotos
          etiqueta="Fotos"
          obligatorio
          requeridas={fotosObligatorias}
          subidas={fotos}
          onSubir={subir}
          error={intentado && fotosFaltantes > 0 ? "Toma todas las fotos" : undefined}
        />
      )}
      {fotosOpcionales.length > 0 && (
        <CampoFotos etiqueta="Fotos (opcional)" requeridas={fotosOpcionales} subidas={fotos} onSubir={subir} />
      )}

      <CampoNota etiqueta="Notas (opcional)" valor={notas} onCambio={setNotas} maximo={500} />
    </Pantalla>
  );
}

/** El control que corresponde al tipo del campo. */
function CampoDeRevision({
  campo,
  captura,
  onCambio,
  error,
}: {
  campo: CampoRevision;
  captura: Captura;
  onCambio: (cambio: Partial<Captura>) => void;
  error?: string;
}) {
  const etiqueta = campo.unidad ? `${campo.nombre} (${campo.unidad})` : campo.nombre;
  const comunes = { etiqueta, obligatorio: campo.requerido, error };

  switch (campo.tipo) {
    case "INSPECCION":
      return (
        <CampoOpciones
          {...comunes}
          opciones={OPCIONES_INSPECCION}
          valor={captura.estado}
          onCambio={(estado) => onCambio({ estado })}
          detalle={captura.detalle}
          onDetalle={(detalle) => onCambio({ detalle })}
        />
      );
    case "BOOLEANO":
      return (
        <CampoSegmentado
          {...comunes}
          opciones={[
            { valor: "Sí", etiqueta: "Sí" },
            { valor: "No", etiqueta: "No" },
          ]}
          valor={captura.valor || null}
          onCambio={(valor) => onCambio({ valor })}
        />
      );
    case "LISTA": {
      const opciones = campo.opciones.map((o) => ({ valor: o, etiqueta: o }));
      // Pocas opciones cortas van en botones; las demás, en lista
      const enBotones = opciones.length <= 4 && campo.opciones.every((o) => o.length <= 6);
      return enBotones ? (
        <CampoSegmentado {...comunes} opciones={opciones} valor={captura.valor || null} onCambio={(valor) => onCambio({ valor })} />
      ) : (
        <CampoOpciones {...comunes} opciones={opciones} valor={captura.valor || null} onCambio={(valor) => onCambio({ valor })} />
      );
    }
    case "ENTERO":
    case "DECIMAL":
      return (
        <CampoTexto
          {...comunes}
          inputMode={campo.tipo === "ENTERO" ? "numeric" : "decimal"}
          placeholder={campo.tipo === "ENTERO" ? "Ej. 3000" : "Ej. 1,284.5"}
          valor={captura.valor}
          onCambio={(valor) => onCambio({ valor })}
        />
      );
    case "FECHA":
      return <CampoTexto {...comunes} type="date" valor={captura.valor} onCambio={(valor) => onCambio({ valor })} />;
    default:
      return <CampoTexto {...comunes} maxLength={120} valor={captura.valor} onCambio={(valor) => onCambio({ valor })} />;
  }
}

/** Lo que le falta a un campo, o null si está bien. El backend vuelve a revisar con las mismas reglas. */
function errorDeCampo(campo: CampoRevision, captura: Captura): string | null {
  if (campo.tipo === "INSPECCION") {
    if (!captura.estado) return campo.requerido ? "Marca cómo está" : null;
    if (captura.estado !== "BIEN" && !captura.detalle.trim()) return "Describe lo que encontraste";
    return null;
  }
  const texto = captura.valor.trim();
  if (!texto) return campo.requerido ? "Llena este campo" : null;
  if (campo.tipo === "ENTERO" || campo.tipo === "DECIMAL") {
    const numero = leerNumero(texto);
    if (numero === null || numero < 0) return "Escribe un número, por ejemplo 1284.5";
    if (campo.tipo === "ENTERO" && !Number.isInteger(numero)) return "Escribe un número entero";
  }
  return null;
}

/** El texto tal como lo pide el backend; los números van sin comas ("1,284.5" o "1284,5" → "1284.5"). */
function valorParaEnviar(campo: CampoRevision, texto: string) {
  const limpio = texto.trim();
  if (!limpio) return null;
  if (campo.tipo === "ENTERO" || campo.tipo === "DECIMAL") return String(leerNumero(limpio));
  return limpio;
}

function faltantes(campos: number, fotos: number) {
  const partes: string[] = [];
  if (campos > 0) partes.push(`${campos} campo${campos === 1 ? "" : "s"}`);
  if (fotos > 0) partes.push(`${fotos} foto${fotos === 1 ? "" : "s"}`);
  return `Faltan ${partes.join(" y ")}`;
}

/** Pantalla de cierre según cómo salió la revisión. */
function Cierre({ terminada, onVolver }: { terminada: RevisionTerminada; onVolver: () => void }) {
  const accion = { texto: "Volver a revisiones", onClick: onVolver };

  if (terminada.resultado === "REQUIERE_MANTENIMIENTO") {
    return (
      <Resultado
        tipo="error"
        titulo="Falla reportada"
        texto={`Se creó el mantenimiento de ${terminada.unidad}; el auxiliar lo asigna desde la oficina.`}
        accion={accion}
      />
    );
  }
  if (terminada.tipo === "SALIDA") {
    return (
      <Resultado
        tipo="exito"
        titulo={`${terminada.unidad} listo para entrega`}
        texto="Ya aparece listo en la oficina. Cuando todos los equipos de la renta estén listos, se programa el traslado."
        accion={accion}
      />
    );
  }
  return (
    <Resultado
      tipo="exito"
      titulo={`${terminada.unidad} revisado`}
      texto="El equipo regresó y quedó fuera de la renta."
      accion={accion}
    />
  );
}