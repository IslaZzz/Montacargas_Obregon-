import { useEffect, useState } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { Button } from "../../../components/buttons";
import Chip from "../../../components/chips";
import { useTitulo } from "../../../hooks/useTitulo";
import { ErrorApi } from "../../../services/apiClient";
import { guardarDescuento, guardarTraslado, obtenerCotizacion } from "../cotizacionesApi";
import type { Cotizacion, TipoDescuento } from "../cotizacionesTypes";
import { chipDeEstado, nombreCompleto, textoDias } from "../cotizacionesUtils";
import PasoPrecioEquipos from "./PasoPrecioEquipo";
import PasoTraslado from "./PasoTraslado";
import type { CampoTraslado } from "./PasoTraslado";
import PasoDescuento from "./PasoDescuento";
import type { CampoDescuento } from "./PasoDescuento";
import PasosPrecio, { PASOS_PRECIO } from "./PasosPrecio";
import PasoVigencia from "./PasoVigencia";
import {
  calcularDescuento,
  calcularPrecio,
  leerMonto,
  MENSAJE_DESCUENTO,
  MENSAJE_MONTO,
  MENSAJE_MOTIVO,
  redondear,
} from "./precioCalculo";
import ResumenPrecio from "./ResumenPrecio";
import type { DescuentoResumen } from "./ResumenPrecio";
import "../../../styles/cotizaciones/cotizaciones.css";
import "../../../styles/cotizaciones/nueva-cotizacion.css";
import "../../../styles/cotizaciones/precio-cotizacion.css";

const PASO_EQUIPOS = 0;
const PASO_TRASLADO = 1;
const PASO_DESCUENTO = 2;
const PASO_VIGENCIA = 3;
// Último paso que ya está construido; la Vigencia (HU-17) llega después
const ULTIMO_PASO_LISTO = PASO_VIGENCIA;

type Errores = Partial<Record<CampoTraslado, string>>;
type ErroresDescuento = Partial<Record<CampoDescuento, string>>;

// "lunes 12 de octubre"
const diaLargo = (iso: string) =>
  new Date(iso).toLocaleDateString("es-MX", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

// "Del lunes 12 al viernes 16 de octubre" (si es el mismo mes, el mes se dice una sola vez)
function textoPeriodo(entrega: string, recoleccion: string) {
  const desde = diaLargo(entrega);
  const hasta = diaLargo(recoleccion);
  const mes = (texto: string) => texto.slice(texto.indexOf(" de "));
  const desdeCorto =
    mes(desde) === mes(hasta) ? desde.slice(0, desde.indexOf(" de ")) : desde;
  return `Del ${desdeCorto} al ${hasta}`;
}

export default function PrecioCotizacion() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null);
  const [errorCarga, setErrorCarga] = useState("");

  const [paso, setPaso] = useState(PASO_EQUIPOS);

  // Lo que está escrito en los campos de traslado
  const [traslado, setTraslado] = useState<Record<CampoTraslado, string>>({
    entrega: "0.00",
    recoleccion: "0.00",
  });
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

  useTitulo(
    cotizacion ? `Precio de ${cotizacion.folio}` : "Precio de la cotización",
  );

  useEffect(() => {
    if (!id) return;
    obtenerCotizacion(id)
      .then((c) => {
        setCotizacion(c);
        // Si ya se había capturado un traslado, se muestra
        setTraslado({
          entrega: redondear(c.trasladoEntrega ?? 0).toFixed(2),
          recoleccion: redondear(c.trasladoRecoleccion ?? 0).toFixed(2),
        });
        // Si ya tenía descuento, también
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

  if (!cotizacion) {
    return (
      <div className="pagina">
        <p>Cargando cotización…</p>
      </div>
    );
  }

  const c = cotizacion;

  // El precio solo se arma mientras la cotización es Borrador
  if (c.estado !== "BORRADOR")
    return <Navigate to={`/cotizaciones/${c.id}`} replace />;

  const subtotalEquipos = redondear(
    c.partidas.reduce((suma, p) => suma + p.importe, 0),
  );
  // Un monto inválido cuenta como 0 en el resumen; el error sale al pulsar "Siguiente"
  const montoEntrega = leerMonto(traslado.entrega) ?? 0;
  const montoRecoleccion = leerMonto(traslado.recoleccion) ?? 0;
  const trasladoCapturado =
    paso >= PASO_TRASLADO || montoEntrega > 0 || montoRecoleccion > 0;

  // Subtotal antes del descuento (equipos + traslados) y cuánto se descuenta
  const { subtotal } = calcularPrecio(subtotalEquipos, montoEntrega, montoRecoleccion);
  const montoDescuento = calcularDescuento(tipoDescuento, descuento.valor, subtotal);

  // Anticipo del 30 % con el descuento ya aplicado, para las condiciones del paso 4
  const { anticipo } = calcularPrecio(
    subtotalEquipos,
    montoEntrega,
    montoRecoleccion,
    montoDescuento ?? 0,
  );

  const resumenDescuento: DescuentoResumen =
    paso < PASO_DESCUENTO && tipoDescuento === "NINGUNO"
      ? { tipo: "pendiente" }
      : tipoDescuento === "NINGUNO"
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

  // «Descuento inválido» sale mientras se escribe, como en el prototipo; el motivo, al guardar
  const erroresDescuentoVisibles: ErroresDescuento = {
    valor:
      erroresDescuento.valor ??
      (tipoDescuento !== "NINGUNO" && descuento.valor !== "" && montoDescuento === null
        ? MENSAJE_DESCUENTO
        : undefined),
    motivo: erroresDescuento.motivo,
  };

  const irAPaso = (destino: number) => {
    setPaso(destino);
    setErrorGuardar("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const cambiarTraslado = (campo: CampoTraslado, valor: string) => {
    setTraslado((actual) => ({ ...actual, [campo]: valor }));
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }));
  };

  // Al salir del campo, el monto queda con dos decimales
  const formatearTraslado = (campo: CampoTraslado) => {
    const monto = leerMonto(traslado[campo]);
    if (monto !== null) cambiarTraslado(campo, monto.toFixed(2));
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

  // Valida y guarda el traslado; devuelve true si quedó guardado
  async function guardarTrasladoCapturado() {
    const trasladoEntrega = leerMonto(traslado.entrega);
    const trasladoRecoleccion = leerMonto(traslado.recoleccion);

    const encontrados: Errores = {};
    if (trasladoEntrega === null) encontrados.entrega = MENSAJE_MONTO;
    if (trasladoRecoleccion === null) encontrados.recoleccion = MENSAJE_MONTO;
    setErrores(encontrados);
    if (trasladoEntrega === null || trasladoRecoleccion === null) return false;

    setGuardando(true);
    setErrorGuardar("");
    try {
      await guardarTraslado(c.id, { trasladoEntrega, trasladoRecoleccion });
      return true;
    } catch {
      setErrorGuardar("No se pudo guardar el traslado. Intenta de nuevo.");
      return false;
    } finally {
      setGuardando(false);
    }
  }

  // Revisa el descuento sin guardarlo; devuelve true si se puede seguir (HU-15)
  function validarDescuento() {
    const encontrados: ErroresDescuento = {};
    if (tipoDescuento !== "NINGUNO") {
      if (montoDescuento === null) encontrados.valor = MENSAJE_DESCUENTO;
      if (!descuento.motivo.trim()) encontrados.motivo = MENSAJE_MOTIVO;
    }
    setErroresDescuento(encontrados);
    return Object.keys(encontrados).length === 0;
  }

  // Guarda el descuento al final del paso 4. Con descuento, la cotización pasa a En autorización
  async function guardarDescuentoCapturado() {
    if (!validarDescuento()) {
      irAPaso(PASO_DESCUENTO);
      return false;
    }

    setGuardando(true);
    setErrorGuardar("");
    try {
      await guardarDescuento(
        c.id,
        tipoDescuento === "NINGUNO"
          ? { tipo: "NINGUNO", valor: null, motivo: null }
          : {
            tipo: tipoDescuento,
            valor: Number(descuento.valor),
            motivo: descuento.motivo.trim(),
          },
      );
      return true;
    } catch (err) {
      setErrorGuardar(
        err instanceof ErrorApi
          ? err.message
          : "No se pudo guardar la cotización. Intenta de nuevo.",
      );
      return false;
    } finally {
      setGuardando(false);
    }
  }

  const siguiente = async () => {
    if (paso === PASO_TRASLADO && !(await guardarTrasladoCapturado())) return;
    if (paso === PASO_DESCUENTO && !validarDescuento()) return;
    if (paso === PASO_VIGENCIA && !(await guardarDescuentoCapturado())) return;

    if (paso < ULTIMO_PASO_LISTO) irAPaso(paso + 1);
    // Al terminar el paso 4 se va al detalle con el aviso «Cotización guardada»
    else navigate(`/cotizaciones/${c.id}`, { state: { precioGuardado: true } });
  };

  // En el paso 4, con descuento, el botón avisa que va a gerencia (HU-15, criterio 5)
  const textoSiguiente = guardando
    ? "Guardando…"
    : paso === PASO_VIGENCIA
      ? tipoDescuento === "NINGUNO"
        ? "Guardar cotización"
        : "Guardar y enviar a autorización"
      : "Siguiente →";

  const aviso =
    paso === PASO_EQUIPOS
      ? {
        titulo: `La renta dura ${textoDias(c.dias)}`,
        texto: `${textoPeriodo(c.fechaEntrega, c.fechaRecoleccion)}. Se toma la tarifa vigente. El sistema hace las cuentas de cada equipo.`,
      }
      : paso === PASO_TRASLADO
        ? {
          titulo: "Los traslados son opcionales",
          texto:
            "Si no hay traslado, déjalos en 0.00. Se suman como conceptos separados del importe de cada equipo.",
        }
        : paso === PASO_DESCUENTO
          ? {
            titulo: "El descuento solo cambia esta cotización",
            texto:
              "No cambia las tarifas del catálogo ni el precio de cada equipo. Si agregas un descuento, gerencia lo tiene que autorizar antes de mandarlo al cliente.",
          }
          : null; // El paso 4 no lleva aviso

  return (
    <div className="pagina-form">
      <div className="pagina nueva-cotizacion precio-cotizacion">
        {/* Título y estado */}
        <div className="detalle-titulo-texto">
          <div className="detalle-titulo-folio">
            <h1>Precio de la cotización</h1>
            <Chip category="cotizacion" status={chipDeEstado(c.estado)} />
          </div>
          <p>
            {[c.folio, nombreCompleto(c.cliente), c.cliente.empresa]
              .filter(Boolean)
              .join(" · ")}
          </p>
        </div>

        <PasosPrecio actual={paso} />

        {aviso && (
          <div className="aviso-info precio-aviso">
            <span className="material-symbols-rounded" aria-hidden="true">
              info
            </span>
            <div>
              <strong>{aviso.titulo}</strong>
              <p>{aviso.texto}</p>
            </div>
          </div>
        )}

        {errorGuardar && (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>{errorGuardar}</strong>
            </div>
          </div>
        )}

        <div className="detalle-cuerpo">
          {/* Columna izquierda: el paso en el que va */}
          <div className="detalle-principal">
            {paso === PASO_EQUIPOS && (
              <PasoPrecioEquipos
                cotizacion={c}
                subtotalEquipos={subtotalEquipos}
              />
            )}
            {paso === PASO_TRASLADO && (
              <PasoTraslado
                cotizacion={c}
                valores={traslado}
                errores={errores}
                cambiar={cambiarTraslado}
                formatear={formatearTraslado}
              />
            )}
            {paso === PASO_DESCUENTO && (
              <PasoDescuento
                cotizacion={c}
                tipo={tipoDescuento}
                valores={descuento}
                errores={erroresDescuentoVisibles}
                subtotal={subtotal}
                montoDescuento={montoDescuento}
                elegirTipo={elegirTipoDescuento}
                cambiar={cambiarDescuento}
              />
            )}
            {paso === PASO_VIGENCIA && (
              <PasoVigencia cotizacion={c} anticipo={anticipo} />
            )}
          </div>

          {/* Columna derecha: resumen del precio, se actualiza con lo que se captura */}
          <div className="detalle-lateral">
            <ResumenPrecio
              dias={c.dias}
              subtotalEquipos={subtotalEquipos}
              trasladoEntrega={montoEntrega}
              trasladoRecoleccion={montoRecoleccion}
              trasladoCapturado={trasladoCapturado}
              descuento={resumenDescuento}
            />
          </div>
        </div>
      </div>

      <div className="barra-acciones">
        <p>
          Paso {paso + 1} de {PASOS_PRECIO.length} ·{" "}
          <span className="nc-barra-paso">{PASOS_PRECIO[paso]}</span>
        </p>
        {paso === PASO_EQUIPOS ? (
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate("/cotizaciones")}
          >
            Guardar y salir
          </Button>
        ) : (
          <Button
            type="button"
            variant="secondary"
            disabled={guardando}
            onClick={() => irAPaso(paso - 1)}
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              arrow_back
            </span>
            Anterior
          </Button>
        )}
        <Button
          type="button"
          variant="primary"
          disabled={guardando}
          onClick={siguiente}
        >
          {textoSiguiente}
        </Button>
      </div>
    </div>
  );
}