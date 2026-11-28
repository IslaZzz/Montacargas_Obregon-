import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { Button } from "../../../components/buttons";
import Chip from "../../../components/chips";
import { useTitulo } from "../../../hooks/useTitulo";
import { ErrorApi } from "../../../services/apiClient";
import {
  formatoDireccion,
  formatoFechaHora,
  formatoMoneda,
  formatoTelefono,
} from "../../../utils/formato";
import { corregirCotizacion, obtenerCotizacion } from "../cotizacionesApi";
import type {
  CorregirCotizacionRequest,
  Cotizacion,
  TipoDescuento,
} from "../cotizacionesTypes";
import { chipDeEstado, nombreCompleto, textoDias } from "../cotizacionesUtils";
import { calcularDias, fechaHoraIso, importeCentavos } from "../nueva/calculo";
import { listarEquiposOperativos } from "../nueva/nuevaCotizacionApi";
import { direccionObra } from "../nueva/nuevaCotizacionTypes";
import type {
  BorradorCotizacion,
  EquipoCatalogo,
  Errores,
} from "../nueva/nuevaCotizacionTypes";
import PasoEquipos from "../nueva/PasoEquipos";
import PasoFechasObra from "../nueva/PasoFechasObra";
import {
  erroresDelBackend,
  hayErrores,
  pasoConErrores,
  validarPaso,
} from "../nueva/validacion";
import PasoDescuento from "../precio/PasoDescuento";
import type { CampoDescuento } from "../precio/PasoDescuento";
import {
  calcularDescuento,
  calcularPrecio,
  MENSAJE_DESCUENTO,
  MENSAJE_MOTIVO,
  redondear,
} from "../precio/precioCalculo";
import ResumenPrecio from "../precio/ResumenPrecio";
import type { DescuentoResumen } from "../precio/ResumenPrecio";
import "../../../styles/cotizaciones/cotizaciones.css";
import "../../../styles/cotizaciones/nueva-cotizacion.css";
import "../../../styles/cotizaciones/precio-cotizacion.css";

// Los pasos 2 a 4 de «Nueva cotización» (HU-12) más el descuento, que es lo que revisa gerencia.
// El cliente y la razón social no cambian.
const PASOS = ["Fechas y obra", "Equipos", "Descuento", "Revisar y reenviar"];
const PASO_FECHAS = 0;
const PASO_EQUIPOS = 1;
const PASO_DESCUENTO = 2;
const PASO_REVISAR = 3;
// validacion.ts cuenta los pasos desde «Cliente»; aquí el primero es «Fechas y obra»
const DESFASE_HU12 = 1;

type ErroresDescuento = Partial<Record<CampoDescuento, string>>;

// La cotización como la edita «Nueva cotización»: fecha y hora por separado y la dirección campo por campo.
// Solo quedan los equipos que se pueden rentar hoy
function aBorrador(c: Cotizacion, operativos: EquipoCatalogo[]): BorradorCotizacion {
  const porId = new Map(operativos.map((e) => [e.id, e]));
  const d = c.direccionObra;
  return {
    cliente: c.cliente,
    razonSocialId: c.razonSocial.id,
    fechaEntrega: c.fechaEntrega.slice(0, 10),
    horaEntrega: c.fechaEntrega.slice(11, 16),
    fechaRecoleccion: c.fechaRecoleccion.slice(0, 10),
    horaRecoleccion: c.fechaRecoleccion.slice(11, 16),
    calleObra: d.calle,
    numeroObra: d.numero,
    coloniaObra: d.colonia,
    ciudadObra: d.ciudad,
    estadoObra: d.estado ?? "",
    codigoPostalObra: d.codigoPostal,
    contactoObra: c.contactoObra,
    telefonoContacto: c.telefonoContacto,
    equipos: c.partidas.flatMap((p) => porId.get(p.equipoId) ?? []),
  };
}

function aSolicitud(
  b: BorradorCotizacion,
  tipo: TipoDescuento,
  descuento: Record<CampoDescuento, string>,
): CorregirCotizacionRequest {
  const conDescuento = tipo !== "NINGUNO";
  return {
    fechaEntrega: fechaHoraIso(b.fechaEntrega, b.horaEntrega),
    fechaRecoleccion: fechaHoraIso(b.fechaRecoleccion, b.horaRecoleccion),
    direccionObra: direccionObra(b),
    contactoObra: b.contactoObra.trim(),
    telefonoContacto: b.telefonoContacto,
    equipoIds: b.equipos.map((e) => e.id),
    descuentoTipo: tipo,
    descuentoValor: conDescuento ? Number(descuento.valor) : null,
    descuentoMotivo: conDescuento ? descuento.motivo.trim() : null,
  };
}

const textoDeErrores = (total: number) =>
  `Hay ${total} ${total === 1 ? "dato" : "datos"} por corregir`;

// "MC-07 ya no está disponible", "MC-07 y GR-02 ya no están disponibles"
const textoQuitados = (unidades: string[]) =>
  unidades.length === 1
    ? `${unidades[0]} ya no está disponible para rentar, así que se quitó.`
    : `${unidades.slice(0, -1).join(", ")} y ${unidades.at(-1)} ya no están disponibles para rentar, así que se quitaron.`;

// «Corregir cotización» (HU-32): se abre desde el detalle de una cotización Rechazada
export default function CorregirCotizacion() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null);
  const [borrador, setBorrador] = useState<BorradorCotizacion | null>(null);
  // Unidades de la cotización que ya no se pueden rentar
  const [quitados, setQuitados] = useState<string[]>([]);
  const [errorCarga, setErrorCarga] = useState("");

  const [paso, setPaso] = useState(PASO_FECHAS);
  const [errores, setErrores] = useState<Errores>({});

  // Descuento (HU-15): la opción elegida y lo escrito en sus campos
  const [tipoDescuento, setTipoDescuento] = useState<TipoDescuento>("NINGUNO");
  const [descuento, setDescuento] = useState<Record<CampoDescuento, string>>({
    valor: "",
    motivo: "",
  });
  const [erroresDescuento, setErroresDescuento] = useState<ErroresDescuento>({});

  const [errorGuardar, setErrorGuardar] = useState("");
  const [guardando, setGuardando] = useState(false);

  useTitulo(cotizacion ? `Corregir ${cotizacion.folio}` : "Corregir cotización");

  // Carga la cotización con sus datos actuales
  useEffect(() => {
    if (!id) return;
    Promise.all([obtenerCotizacion(id), listarEquiposOperativos()])
      .then(([c, operativos]) => {
        const nuevo = aBorrador(c, operativos);
        const elegidos = new Set(nuevo.equipos.map((e) => e.id));
        setCotizacion(c);
        setBorrador(nuevo);
        setQuitados(c.partidas.filter((p) => !elegidos.has(p.equipoId)).map((p) => p.unidad));
        setTipoDescuento(c.descuentoTipo);
        setDescuento({
          valor: c.descuentoValor === null ? "" : String(c.descuentoValor),
          motivo: c.descuentoMotivo ?? "",
        });
      })
      .catch(() => setErrorCarga("No fue posible cargar la cotización."));
  }, [id]);

  if (errorCarga) {
    return (
      <div className="pagina">
        <div className="aviso-error" role="alert">
          <span className="material-symbols-rounded">error</span>
          <div>
            <strong>{errorCarga}</strong>
          </div>
        </div>
      </div>
    );
  }

  if (!cotizacion || !borrador) {
    return (
      <div className="pagina">
        <p>Cargando cotización…</p>
      </div>
    );
  }

  const c = cotizacion;
  const b = borrador;

  // Solo se corrige una Rechazada; si ya se corrigió, se regresa al detalle
  if (c.estado !== "RECHAZADA") return <Navigate to={`/cotizaciones/${c.id}`} replace />;

  // El precio con las fechas y los equipos que se están editando; los traslados no cambian
  const dias = calcularDias(b.fechaEntrega, b.fechaRecoleccion) ?? 0;
  const partidas = b.equipos.map((e) => ({
    id: e.id,
    unidad: e.numeroUnidad,
    modelo: e.modelo.nombre,
    tarifaDia: e.modelo.tarifaDia,
    importe: importeCentavos(dias, e.modelo.tarifaDia) / 100,
  }));
  const subtotalEquipos = redondear(partidas.reduce((suma, p) => suma + p.importe, 0));
  const { subtotal } = calcularPrecio(subtotalEquipos, c.trasladoEntrega, c.trasladoRecoleccion);
  const montoDescuento = calcularDescuento(tipoDescuento, descuento.valor, subtotal);
  const conDescuento = tipoDescuento !== "NINGUNO";

  const resumenDescuento: DescuentoResumen = !conDescuento
    ? { tipo: "sin" }
    : montoDescuento === null
      ? { tipo: "invalido" }
      : {
          tipo: "aplicado",
          monto: montoDescuento,
          etiqueta:
            tipoDescuento === "PORCENTAJE"
              ? `Descuento (${Number(descuento.valor)}%)`
              : "Descuento",
        };

  // «Descuento inválido» sale mientras se escribe, como en «Precio de la cotización»; el motivo, al seguir
  const erroresDescuentoVisibles: ErroresDescuento = {
    valor:
      erroresDescuento.valor ??
      (conDescuento && descuento.valor !== "" && montoDescuento === null
        ? MENSAJE_DESCUENTO
        : undefined),
    motivo: erroresDescuento.motivo,
  };

  const cambiar = (cambios: Partial<BorradorCotizacion>) => {
    setBorrador((actual) => (actual ? { ...actual, ...cambios } : actual));
    setErrores((actuales) => {
      const restantes = { ...actuales };
      for (const campo of Object.keys(cambios) as (keyof BorradorCotizacion)[])
        delete restantes[campo];
      return restantes;
    });
  };

  const irAPaso = (destino: number) => {
    setPaso(destino);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // Al cambiar de opción se borra el valor: 10 no significa lo mismo en % que en pesos
  const elegirTipoDescuento = (tipo: TipoDescuento) => {
    setTipoDescuento(tipo);
    setDescuento((actual) => ({ ...actual, valor: "" }));
    setErroresDescuento({});
  };

  const cambiarDescuento = (campo: CampoDescuento, valor: string) => {
    setDescuento((actual) => ({ ...actual, [campo]: valor }));
    setErroresDescuento((actuales) => ({ ...actuales, [campo]: undefined }));
  };

  // Revisa el descuento; devuelve true si se puede seguir
  function validarDescuento() {
    const encontrados: ErroresDescuento = {};
    if (conDescuento) {
      if (montoDescuento === null) encontrados.valor = MENSAJE_DESCUENTO;
      if (!descuento.motivo.trim()) encontrados.motivo = MENSAJE_MOTIVO;
    }
    setErroresDescuento(encontrados);
    return Object.keys(encontrados).length === 0;
  }

  const siguiente = () => {
    if (paso === PASO_DESCUENTO) {
      if (!validarDescuento()) return;
    } else {
      const encontrados = validarPaso(paso + DESFASE_HU12, b);
      setErrores(encontrados);
      if (hayErrores(encontrados)) return;
    }
    setErrorGuardar("");
    irAPaso(paso + 1);
  };

  const anterior = () => {
    setErrores({});
    setErrorGuardar("");
    if (paso === PASO_FECHAS) navigate(`/cotizaciones/${c.id}`);
    else irAPaso(paso - 1);
  };

  async function guardar() {
    if (!validarDescuento()) return irAPaso(PASO_DESCUENTO);
    setGuardando(true);
    setErrorGuardar("");
    try {
      await corregirCotizacion(c.id, aSolicitud(b, tipoDescuento, descuento));
      // El detalle muestra «Cotización corregida» y qué sigue
      navigate(`/cotizaciones/${c.id}`, {
        state: { precioGuardado: true, corregida: true },
      });
    } catch (error) {
      const campos = error instanceof ErrorApi ? error.errores : {};
      const delBackend = erroresDelBackend(campos);
      setErrores(delBackend);
      setErroresDescuento({ valor: campos.descuentoValor, motivo: campos.descuentoMotivo });
      setErrorGuardar(
        error instanceof Error ? error.message : "No se pudo guardar la cotización.",
      );
      const pasoHu12 = pasoConErrores(delBackend);
      if (pasoHu12 !== null) irAPaso(pasoHu12 - DESFASE_HU12);
      else if (campos.descuentoValor || campos.descuentoMotivo) irAPaso(PASO_DESCUENTO);
      setGuardando(false);
    }
  }

  const totalErrores = Object.keys(errores).length;
  const rechazo = c.autorizacion;

  return (
    <div className="pagina-form">
      <div className="pagina nueva-cotizacion precio-cotizacion">
        <div className="detalle-titulo-texto">
          <div className="detalle-titulo-folio">
            <h1>Corregir cotización</h1>
            <Chip category="cotizacion" status={chipDeEstado(c.estado)} />
          </div>
          <p>
            {[c.folio, nombreCompleto(c.cliente), c.cliente.empresa]
              .filter(Boolean)
              .join(" · ")}
          </p>
        </div>

        {/* Lo que dijo gerencia, a la vista en todos los pasos */}
        {rechazo?.comentario && (
          <div className="aviso-error" role="status">
            <span className="material-symbols-rounded" aria-hidden="true">
              cancel
            </span>
            <div>
              <strong>Comentario de gerencia: «{rechazo.comentario}»</strong>
              <p>
                Ajusta lo necesario y vuelve a guardar. Si sigue con descuento,
                regresa a autorización; si lo quitas, queda en Borrador.
              </p>
            </div>
          </div>
        )}

        <ol className="nc-pasos">
          {PASOS.map((nombre, i) => (
            <li
              key={nombre}
              className={i === paso ? "activo" : i < paso ? "hecho" : undefined}
              aria-current={i === paso ? "step" : undefined}
            >
              <span className="nc-pasos-numero">{i < paso ? "✓" : i + 1}</span>
              {nombre}
            </li>
          ))}
        </ol>

        {quitados.length > 0 && paso <= PASO_EQUIPOS && (
          <div className="aviso-info">
            <span className="material-symbols-rounded" aria-hidden="true">
              info
            </span>
            <p>{textoQuitados(quitados)} Agrega otro en el paso de equipos.</p>
          </div>
        )}

        {(totalErrores > 0 || errorGuardar) && (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>{errorGuardar || textoDeErrores(totalErrores)}</strong>
              <p>
                {totalErrores > 0
                  ? (errores.equipos ?? "Revisa los campos marcados en rojo.")
                  : "Intenta de nuevo. Si sigue fallando, revisa que el servidor esté encendido."}
              </p>
            </div>
          </div>
        )}

        {paso === PASO_FECHAS && (
          <PasoFechasObra borrador={b} cambiar={cambiar} errores={errores} />
        )}
        {paso === PASO_EQUIPOS && <PasoEquipos borrador={b} cambiar={cambiar} />}

        {paso >= PASO_DESCUENTO && (
          <div className="detalle-cuerpo">
            <div className="detalle-principal">
              {paso === PASO_DESCUENTO ? (
                <PasoDescuento
                  partidas={partidas}
                  dias={dias}
                  tipo={tipoDescuento}
                  valores={descuento}
                  errores={erroresDescuentoVisibles}
                  subtotal={subtotal}
                  montoDescuento={montoDescuento}
                  elegirTipo={elegirTipoDescuento}
                  cambiar={cambiarDescuento}
                />
              ) : (
                <Revisar
                  borrador={b}
                  dias={dias}
                  partidas={partidas}
                  tipoDescuento={tipoDescuento}
                  valorDescuento={descuento.valor}
                  motivo={descuento.motivo}
                  montoDescuento={montoDescuento ?? 0}
                  irAPaso={irAPaso}
                />
              )}
            </div>

            {/* Resumen del precio, se actualiza con lo que se corrige */}
            <div className="detalle-lateral">
              <ResumenPrecio
                dias={dias}
                subtotalEquipos={subtotalEquipos}
                trasladoEntrega={c.trasladoEntrega}
                trasladoRecoleccion={c.trasladoRecoleccion}
                trasladoCapturado
                descuento={resumenDescuento}
              />
            </div>
          </div>
        )}
      </div>

      <div className="barra-acciones">
        <p>
          Paso {paso + 1} de {PASOS.length} ·{" "}
          <span className="nc-barra-paso">{PASOS[paso]}</span>
        </p>
        <Button type="button" variant="secondary" disabled={guardando} onClick={anterior}>
          {paso === PASO_FECHAS ? "Cancelar" : "Anterior"}
        </Button>
        {paso < PASO_REVISAR ? (
          <Button type="button" variant="primary" onClick={siguiente}>
            Siguiente
          </Button>
        ) : (
          <Button type="button" variant="primary" disabled={guardando} onClick={guardar}>
            {guardando
              ? "Guardando…"
              : conDescuento
                ? "Guardar y reenviar a autorización"
                : "Guardar cotización"}
          </Button>
        )}
      </div>
    </div>
  );
}

interface RevisarProps {
  borrador: BorradorCotizacion;
  dias: number;
  partidas: { id: number; unidad: string; modelo: string; tarifaDia: number; importe: number }[];
  tipoDescuento: TipoDescuento;
  valorDescuento: string;
  motivo: string;
  montoDescuento: number;
  irAPaso: (paso: number) => void;
}

// Último paso: lo corregido, con «Cambiar» para volver a cada paso
function Revisar({
  borrador: b,
  dias,
  partidas,
  tipoDescuento,
  valorDescuento,
  motivo,
  montoDescuento,
  irAPaso,
}: RevisarProps) {
  const conDescuento = tipoDescuento !== "NINGUNO";

  return (
    <>
      <div className="aviso-info">
        <span className="material-symbols-rounded" aria-hidden="true">
          info
        </span>
        <p>
          {conDescuento
            ? "Al guardar, la cotización vuelve a «En autorización» y gerencia recibe la solicitud otra vez."
            : "Ya no lleva descuento: al guardar queda en «Borrador» y ya puedes generar el PDF."}
        </p>
      </div>

      <Seccion titulo="Fechas y lugar" paso={PASO_FECHAS} irAPaso={irAPaso}>
        <dl className="datos">
          <Dato etiqueta="Entrega">
            {formatoFechaHora(fechaHoraIso(b.fechaEntrega, b.horaEntrega))}
          </Dato>
          <Dato etiqueta="Recolección">
            {formatoFechaHora(fechaHoraIso(b.fechaRecoleccion, b.horaRecoleccion))}
          </Dato>
          <Dato etiqueta="Duración">{textoDias(dias)}</Dato>
          <Dato etiqueta="Lugar de entrega">{formatoDireccion(direccionObra(b))}</Dato>
          <Dato etiqueta="Contacto en la obra">
            {b.contactoObra} · {formatoTelefono(b.telefonoContacto)}
          </Dato>
        </dl>
      </Seccion>

      <Seccion titulo={`Equipos (${partidas.length})`} paso={PASO_EQUIPOS} irAPaso={irAPaso}>
        {partidas.map((p) => (
          <div key={p.id} className="equipo-cotizado">
            <strong>
              {p.unidad} · {p.modelo}
            </strong>
            <span>
              {textoDias(dias)} × {formatoMoneda(p.tarifaDia)} = {formatoMoneda(p.importe)}
            </span>
          </div>
        ))}
      </Seccion>

      <Seccion titulo="Descuento" paso={PASO_DESCUENTO} irAPaso={irAPaso}>
        <dl className="datos">
          <Dato etiqueta="Descuento">
            {!conDescuento
              ? "Sin descuento"
              : tipoDescuento === "PORCENTAJE"
                ? `${Number(valorDescuento)}% (−${formatoMoneda(montoDescuento)})`
                : `−${formatoMoneda(montoDescuento)}`}
          </Dato>
          {conDescuento && <Dato etiqueta="Motivo">{motivo.trim()}</Dato>}
        </dl>
      </Seccion>
    </>
  );
}

function Seccion({
  titulo,
  paso,
  irAPaso,
  children,
}: {
  titulo: string;
  paso: number;
  irAPaso: (paso: number) => void;
  children: ReactNode;
}) {
  return (
    <section className="tarjeta">
      <div className="nc-tarjeta-encabezado">
        <h2>{titulo}</h2>
        <Button type="button" variant="tertiary" onClick={() => irAPaso(paso)}>
          Cambiar
        </Button>
      </div>
      {children}
    </section>
  );
}

function Dato({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return (
    <div className="dato">
      <dt>{etiqueta}</dt>
      <dd>{children}</dd>
    </div>
  );
}
