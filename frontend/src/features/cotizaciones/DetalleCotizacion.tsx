import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import Chip from "../../components/chips";
import Modal from "../../components/Modal";
import { useTitulo } from "../../hooks/useTitulo";
import {
  aFecha,
  formatoFecha,
  formatoFechaHora,
  formatoMoneda,
  formatoDireccion,
  formatoTelefono,
} from "../../utils/formato";
import BotonVerPdf from "./BotonVerPdf";
import { cambiarEstadoCotizacion, obtenerCotizacion } from "./cotizacionesApi";
import type { Cotizacion } from "./cotizacionesTypes";
import {
  chipDeEstado,
  ESTADOS_RENTA,
  nombreCompleto,
  textoDias,
} from "./cotizacionesUtils";
import "../../styles/cotizaciones/cotizaciones.css";

type Respuesta = "ACEPTADA" | "NO_ACEPTADA";

const ANTICIPO = 0.3;

// Redondea a centavos para que las cuentas no arrastren decimales
const redondear = (pesos: number) => Math.round(pesos * 100) / 100;

// "Enviada el 08/10/2026", según el estado
function textoFechaEstado(c: Cotizacion) {
  const conFecha = (texto: string, fecha: string | null) =>
    fecha ? `${texto} el ${formatoFecha(fecha)}` : texto;

  switch (c.estado) {
    case "ENVIADA":
    case "VENCIDA":
      return conFecha("Enviada", c.fechaEnvio);
    case "ACEPTADA":
      return conFecha("Aceptada", c.fechaRespuesta);
    case "NO_ACEPTADA":
      return conFecha("No aceptada", c.fechaRespuesta);
    default:
      // Borrador, En autorización y Rechazada
      return conFecha("Creada", c.fechaCreacion);
  }
}

const diaYMes = (texto: string) =>
  aFecha(texto).toLocaleDateString("es-MX", { day: "numeric", month: "long" });

function hora(texto: string) {
  const fecha = aFecha(texto);
  const horas = fecha.getHours();
  const minutos = String(fecha.getMinutes()).padStart(2, "0");
  return `${horas % 12 || 12}:${minutos} ${horas < 12 ? "a. m." : "p. m."}`;
}

const esHoy = (texto: string) => aFecha(texto).toDateString() === new Date().toDateString();

const momento = (texto: string) =>
  `${esHoy(texto) ? "Hoy" : formatoFecha(texto)}, ${hora(texto)}`;

const momentoEnTexto = (texto: string) =>
  `${esHoy(texto) ? "hoy" : `el ${formatoFecha(texto)}`} a las ${hora(texto)}`;

const textoDescuento = (c: Cotizacion) =>
  c.descuentoTipo === "PORCENTAJE"
    ? `del ${c.descuentoValor}%`
    : `de ${formatoMoneda(c.descuentoMonto)}`;

export default function DetalleCotizacion() {
  const { id } = useParams();
  const navigate = useNavigate();
  // Lo que manda la pantalla de la que venimos:
  // - creada: el folio de la cotización recién creada en "Nueva cotización" (HU-12)
  // - precioGuardado: se acaba de guardar en "Precio de la cotización" (HU-13 a HU-17)
  const estadoNavegacion = useLocation().state as {
    creada?: string;
    precioGuardado?: boolean;
  } | null;
  const folioCreado = estadoNavegacion?.creada;
  const recienGuardada = Boolean(estadoNavegacion?.precioGuardado);

  const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null);
  const [error, setError] = useState("");

  // Respuesta que se está confirmando en la ventana; null si está cerrada
  const [confirmando, setConfirmando] = useState<Respuesta | null>(null);
  const [guardando, setGuardando] = useState(false);

  // True justo después de marcar la respuesta: muestra el aviso de arriba
  const [recienMarcada, setRecienMarcada] = useState(false);

  useTitulo(cotizacion ? `Cotización ${cotizacion.folio}` : "Cotización");

  useEffect(() => {
    if (!id) return;
    obtenerCotizacion(id)
      .then(setCotizacion)
      .catch(() => setError("No fue posible cargar la cotización."));
  }, [id]);

  // Mientras se guarda, la ventana no se cierra
  const cerrarConfirmacion = () => {
    if (!guardando) setConfirmando(null);
  };

  // Guarda la respuesta del cliente después de confirmarla
  async function marcar(respuesta: Respuesta) {
    if (!cotizacion) return;
    setGuardando(true);
    setError("");
    try {
      setCotizacion(await cambiarEstadoCotizacion(cotizacion.id, respuesta));
      setRecienMarcada(true);
    } catch {
      setError(
        "No se pudo guardar la respuesta del cliente. Intenta de nuevo.",
      );
    } finally {
      setGuardando(false);
      setConfirmando(null);
    }
  }

  const avisoError = error && (
    <div className="aviso-error" role="alert">
      <span className="material-symbols-rounded">error</span>
      <div>
        <strong>{error}</strong>
      </div>
    </div>
  );

  if (!cotizacion) {
    return (
      <div className="pagina detalle-cotizacion">
        <VolverACotizaciones />
        {avisoError || <p>Cargando cotización…</p>}
      </div>
    );
  }

  const c = cotizacion;
  const empresa = c.cliente.empresa;
  const respondida = c.estado === "ACEPTADA" || c.estado === "NO_ACEPTADA";
  const esBorrador = c.estado === "BORRADOR";
  const enAutorizacion = c.estado === "EN_AUTORIZACION";
  const rechazada = c.estado === "RECHAZADA";
  const vencida = c.estado === "VENCIDA";
  const tieneDescuento = c.descuentoTipo !== "NINGUNO";

  // Respuesta de gerencia al descuento (HU-16)
  const autorizacion = c.autorizacion;
  const autorizada = esBorrador && autorizacion?.resultado === "AUTORIZADA";
  const respuestaGerencia =
    autorizacion && (autorizada || rechazada) ? autorizacion : null;

  // Borrador al que todavía le falta el precio: se sigue con "Continuar con el precio"
  const faltaPrecio = esBorrador && !c.precioDefinido;
  // Borrador con el precio ya guardado: se puede generar el PDF y enviarla
  const precioListo = esBorrador && c.precioDefinido;
  // En autorización, BotonVerPdf se muestra deshabilitado con «Esperando autorización» (HU-16).
  const mostrarPdf = !faltaPrecio && !rechazada;

  // Totales
  const subtotalEquipos = redondear(
    c.partidas.reduce((suma, p) => suma + p.importe, 0),
  );
  const traslado = redondear(
    (c.trasladoEntrega ?? 0) + (c.trasladoRecoleccion ?? 0),
  );
  const mostrarDesglose = c.precioDefinido || traslado > 0;
  const anticipo = redondear(c.total * ANTICIPO);

  const datosCliente = (
    <>
      <Dato etiqueta="Cliente">
        {[nombreCompleto(c.cliente), empresa].filter(Boolean).join(" · ")}
      </Dato>
      <Dato etiqueta="Se factura a">
        {c.razonSocial.nombre} (RFC: {c.razonSocial.rfc})
      </Dato>
    </>
  );

  const datosEntrega = (
    <>
      <Dato etiqueta="Entrega">{formatoFechaHora(c.fechaEntrega)}</Dato>
      <Dato etiqueta="Recolección">{formatoFechaHora(c.fechaRecoleccion)}</Dato>
      {faltaPrecio && <Dato etiqueta="Duración">{textoDias(c.dias)}</Dato>}
      <Dato
        etiqueta={faltaPrecio ? "Dirección de la obra" : "Lugar de entrega"}
      >
        {formatoDireccion(c.direccionObra)}
      </Dato>
      <Dato etiqueta="Contacto en la obra">
        {c.contactoObra} · {formatoTelefono(c.telefonoContacto)}
      </Dato>
      {c.renta && (
        <Dato etiqueta="Renta">
          {c.renta.folio} · {ESTADOS_RENTA[c.renta.estado]}
        </Dato>
      )}
    </>
  );

  const tarjetaEquipos = (
    <section className="tarjeta detalle-equipos">
      <h2>Equipos ({c.partidas.length})</h2>
      {c.partidas.map((p) => (
        <div key={p.id} className="equipo-cotizado">
          <strong>
            {p.unidad} · {p.modelo}
          </strong>
          {faltaPrecio ? (
            <span>Tarifa vigente: {formatoMoneda(p.tarifaDia)} por día</span>
          ) : (
            <span>
              {textoDias(c.dias)} × {formatoMoneda(p.tarifaDia)} ={" "}
              {formatoMoneda(p.importe)}
            </span>
          )}
        </div>
      ))}
    </section>
  );

  return (
    <div className="pagina detalle-cotizacion">
      {!(faltaPrecio && folioCreado) && <VolverACotizaciones />}

      {/* Aviso al llegar desde "Nueva cotización" (HU-12) */}
      {faltaPrecio && folioCreado && (
        <div className="aviso-exito" role="status">
          <span className="material-symbols-rounded">check_circle</span>
          <div>
            <strong>Cotización guardada con el folio {folioCreado}</strong>
            <p>
              Quedó en Borrador. Puedes seguir con el precio ahora o después
              desde Cotizaciones.
            </p>
          </div>
        </div>
      )}

      {/* Aviso al terminar "Precio de la cotización" (HU-15 y HU-17) */}
      {recienGuardada && (enAutorizacion || precioListo) && (
        <div className="aviso-exito" role="status">
          <span className="material-symbols-rounded">check_circle</span>
          <div>
            <strong>Cotización {c.folio} guardada</strong>
            <p>
              {enAutorizacion
                ? `Lleva un descuento ${textoDescuento(c)}, así que pasó a «En autorización». Gerencia ya recibió la solicitud y te avisaremos en Avisos cuando responda.`
                : "Ya puedes generar el PDF y enviarla al cliente."}
            </p>
          </div>
        </div>
      )}

      {/* Esperando la respuesta de gerencia (HU-16) */}
      {enAutorizacion && !recienGuardada && (
        <div className="aviso-espera" role="status">
          <span className="material-symbols-rounded" aria-hidden="true">
            info
          </span>
          <div>
            <strong>Esperando autorización de gerencia</strong>
            <p>
              Esta cotización lleva un descuento {textoDescuento(c)}. No se
              puede enviar al cliente hasta que gerencia lo autorice. Se te
              notificará en Avisos.
            </p>
          </div>
        </div>
      )}

      {/* Gerencia autorizó: la cotización regresó a Borrador (HU-16) */}
      {autorizada && autorizacion.respondidaPor && autorizacion.fechaRespuesta && (
        <div className="aviso-exito" role="status">
          <span className="material-symbols-rounded">check_circle</span>
          <div>
            <strong>Gerencia autorizó el descuento</strong>
            <p>
              Autorizado por {autorizacion.respondidaPor}{" "}
              {momentoEnTexto(autorizacion.fechaRespuesta)}. Ya puedes generar
              el PDF y enviarla al cliente.
            </p>
          </div>
        </div>
      )}

      {/* Gerencia rechazó (HU-16) */}
      {rechazada && autorizacion?.respondidaPor && autorizacion.fechaRespuesta && (
        <div className="aviso-error" role="status">
          <span className="material-symbols-rounded">cancel</span>
          <div>
            <strong>Rechazada: {autorizacion.comentario}</strong>
            <p>
              {autorizacion.respondidaPor} rechazó el descuento{" "}
              {momentoEnTexto(autorizacion.fechaRespuesta)}. Esta cotización ya
              no se puede enviar al cliente.
            </p>
          </div>
        </div>
      )}

      {/* Aviso después de marcar la respuesta del cliente */}
      {recienMarcada && c.estado === "ACEPTADA" && (
        <div className="aviso-exito" role="status">
          <span className="material-symbols-rounded">check_circle</span>
          <div>
            <strong>Cotización aceptada</strong>
            {/* Al aceptarla nace su renta para Operativo (HU-37) */}
            {c.renta && <p>Renta {c.renta.folio} creada.</p>}
          </div>
        </div>
      )}
      {recienMarcada && c.estado === "NO_ACEPTADA" && (
        <div className="aviso-error" role="status">
          <span className="material-symbols-rounded">error</span>
          <div>
            <strong>Cotización no aceptada</strong>
            <p>
              Se marcó hoy, {formatoFecha(c.fechaRespuesta!)}. Si el cliente
              pide otra propuesta, crea una cotización nueva.
            </p>
          </div>
        </div>
      )}

      {avisoError}

      {/* Título, estado y botones */}
      <div className="detalle-titulo">
        <div className="detalle-titulo-texto">
          <div className="detalle-titulo-folio">
            <h1>
              <span className="solo-laptop">Cotización </span>
              {c.folio}
            </h1>
            <Chip category="cotizacion" status={chipDeEstado(c.estado)} />
            {/* Etiqueta de la HU-16: el descuento ya tiene el visto bueno */}
            {autorizada && (
              <Chip category="cotizacion" status="autorizada">
                Autorizada por gerencia
              </Chip>
            )}
          </div>
          {recienGuardada ? (
            <p>
              Total {formatoMoneda(c.total)} con IVA · Vence el{" "}
              {formatoFecha(c.validaHasta)} · {nombreCompleto(c.cliente)}
            </p>
          ) : (
            <p>
              {nombreCompleto(c.cliente)}
              {empresa && <span className="solo-laptop"> · {empresa}</span>}
              {" · "}
              {textoFechaEstado(c)}
            </p>
          )}
        </div>

        <div className="detalle-titulo-botones">
          {recienGuardada ? (
            <>
              <Link to="/cotizaciones" className="btn btn-tertiary btn-grande">
                Ir a cotizaciones
              </Link>
              {/* Vuelve a abrir el detalle sin el aviso de "guardada" */}
              <button
                type="button"
                className="btn btn-primary btn-grande"
                onClick={() => navigate(`/cotizaciones/${c.id}`, { replace: true })}
              >
                Ver cotización
                <span className="material-symbols-rounded" aria-hidden="true">
                  arrow_forward
                </span>
              </button>
            </>
          ) : (
            <>
              {respondida && (
                <Link to="/cotizaciones" className="btn btn-tertiary btn-grande">
                  Ir a cotizaciones
                </Link>
              )}

              {faltaPrecio && (
                <>
                  <Link to="/clientes" className="btn btn-tertiary btn-grande">
                    Ir a clientes
                  </Link>
                  {/* Abre "Precio de la cotización" (HU-13 a HU-17) */}
                  <Link
                    to={`/cotizaciones/${c.id}/precio`}
                    className="btn btn-primary btn-grande"
                  >
                    Continuar con el precio
                    <span className="material-symbols-rounded" aria-hidden="true">
                      arrow_forward
                    </span>
                  </Link>
                </>
              )}

              {/* En Enviada, en celular el botón va debajo de "¿Qué respondió el cliente?" */}
              {mostrarPdf && (
                <BotonVerPdf
                  cotizacion={c}
                  primario={esBorrador}
                  className={c.estado === "ENVIADA" ? "solo-laptop" : ""}
                />
              )}
            </>
          )}
        </div>
      </div>

      {(c.estado === "ENVIADA" || vencida) && (
        <div className="aviso-info">
          <span className="material-symbols-rounded" aria-hidden="true">
            lock
          </span>
          <p>
            Ya se envió al cliente y no se puede modificar. Si pide cambios, se
            crea una cotización nueva a partir de esta.
          </p>
        </div>
      )}

      <div className="detalle-cuerpo">
        {/* Columna izquierda */}
        <div className="detalle-principal">
          {faltaPrecio ? (
            <>
              <section className="tarjeta detalle-datos">
                <h2>Cliente y facturación</h2>
                <dl className="datos">{datosCliente}</dl>
              </section>
              <section className="tarjeta detalle-datos">
                <h2>Fechas y lugar</h2>
                <dl className="datos">{datosEntrega}</dl>
              </section>
            </>
          ) : (
            <>
              <section className="tarjeta detalle-datos">
                <h2>Cliente y entrega</h2>
                <dl className="datos">
                  {datosCliente}
                  {datosEntrega}
                </dl>
              </section>
              {tarjetaEquipos}
            </>
          )}
        </div>

        {/* Columna derecha */}
        <div className="detalle-lateral">
          {faltaPrecio && tarjetaEquipos}

          {/* Recién guardada: qué sigue (HU-17) */}
          {recienGuardada && (enAutorizacion || precioListo) && (
            <QueSigue cotizacion={c} />
          )}

          {/* La solicitud que espera a gerencia (HU-16) */}
          {enAutorizacion && !recienGuardada && (
            <section className="tarjeta detalle-datos">
              <h2>Solicitud de autorización</h2>
              <dl className="datos">
                {autorizacion && (
                  <Dato etiqueta="Fecha">{momento(autorizacion.fechaSolicitud)}</Dato>
                )}
                {c.descuentoMotivo && (
                  <Dato etiqueta="Motivo del descuento">{c.descuentoMotivo}</Dato>
                )}
              </dl>
            </section>
          )}

          {/* Lo que respondió gerencia (HU-16) */}
          {respuestaGerencia && (
            <section className="tarjeta detalle-datos">
              <h2>Respuesta de gerencia</h2>
              <dl className="datos">
                <Dato etiqueta="Respuesta">
                  {respuestaGerencia.resultado === "AUTORIZADA" ? "Autorizado" : "Rechazado"}
                </Dato>
                {respuestaGerencia.comentario && (
                  <Dato etiqueta="Comentario">{respuestaGerencia.comentario}</Dato>
                )}
              </dl>
            </section>
          )}

          {c.estado === "ENVIADA" && (
            <section className="tarjeta detalle-respuesta">
              <h2>¿Qué respondió el cliente?</h2>
              <p className="detalle-vigencia">
                Válida hasta el {formatoFecha(c.validaHasta)}.
              </p>
              <button
                type="button"
                className="btn btn-primary btn-grande"
                onClick={() => setConfirmando("ACEPTADA")}
              >
                <span className="material-symbols-rounded" aria-hidden="true">
                  check
                </span>
                Marcar como aceptada
              </button>
              <button
                type="button"
                className="btn btn-tertiary btn-grande btn-rojo"
                onClick={() => setConfirmando("NO_ACEPTADA")}
              >
                <span className="material-symbols-rounded" aria-hidden="true">
                  close
                </span>
                Marcar como no aceptada
              </button>
            </section>
          )}

          {/* Pasaron los 7 días sin respuesta (HU-17, criterio 4) */}
          {vencida && (
            <section className="tarjeta detalle-respuesta">
              <h2>¿Qué respondió el cliente?</h2>
              <p className="detalle-vencida">
                <span className="material-symbols-rounded" aria-hidden="true">
                  schedule
                </span>
                Venció el {formatoFecha(c.validaHasta)} sin respuesta del
                cliente. Ya no se puede marcar como aceptada; haz una
                cotización nueva.
              </p>
              <button type="button" className="btn btn-primary btn-grande" disabled>
                <span className="material-symbols-rounded" aria-hidden="true">
                  check
                </span>
                Cotización vencida
              </button>
            </section>
          )}

          {c.estado === "ENVIADA" && (
            <div className="detalle-pdf-celular solo-celular">
              <BotonVerPdf cotizacion={c} />
            </div>
          )}

          {/* Mientras falta el precio no se muestran totales: se ven en el paso siguiente */}
          {!faltaPrecio && (
            <section className="tarjeta detalle-totales">
              <h2>Totales</h2>
              {mostrarDesglose && (
                <>
                  <div className="totales-fila">
                    <span>Equipos ({textoDias(c.dias)})</span>
                    <strong>{formatoMoneda(subtotalEquipos)}</strong>
                  </div>
                  <div className="totales-fila">
                    <span>Traslado de entrega</span>
                    <strong>{formatoMoneda(c.trasladoEntrega ?? 0)}</strong>
                  </div>
                  <div className="totales-fila">
                    <span>Traslado de recolección</span>
                    <strong>{formatoMoneda(c.trasladoRecoleccion ?? 0)}</strong>
                  </div>
                </>
              )}
              <div className="totales-fila">
                <span>Subtotal</span>
                <strong>{formatoMoneda(c.subtotal)}</strong>
              </div>
              {tieneDescuento && (
                <div className="totales-fila totales-descuento">
                  <span>
                    Descuento
                    {c.descuentoTipo === "PORCENTAJE" && ` (${c.descuentoValor}%)`}
                  </span>
                  <strong>−{formatoMoneda(c.descuentoMonto)}</strong>
                </div>
              )}
              <div className="totales-fila">
                <span>IVA (16%)</span>
                <strong>{formatoMoneda(c.iva)}</strong>
              </div>
              <div className="totales-fila totales-total">
                <span>Total con IVA</span>
                <strong>{formatoMoneda(c.total)}</strong>
              </div>
              {(precioListo || enAutorizacion) && (
                <div className="totales-fila">
                  <span>Anticipo para confirmar (30%)</span>
                  <strong>{formatoMoneda(anticipo)}</strong>
                </div>
              )}
            </section>
          )}
        </div>
      </div>

      {/* Confirmación de la respuesta del cliente */}
      <Modal
        abierto={confirmando !== null}
        titulo={
          confirmando === "NO_ACEPTADA"
            ? `¿Marcar la cotización ${c.folio} como no aceptada?`
            : `¿El cliente aceptó la cotización ${c.folio}?`
        }
        onCerrar={cerrarConfirmacion}
        acciones={
          // En celular se apilan, con el de confirmar arriba
          <div className="confirmacion-botones">
            <button
              type="button"
              className="btn btn-tertiary btn-grande"
              disabled={guardando}
              onClick={cerrarConfirmacion}
            >
              No, regresar
            </button>
            {confirmando === "NO_ACEPTADA" ? (
              <button
                type="button"
                className="btn btn-peligro btn-grande"
                disabled={guardando}
                onClick={() => marcar("NO_ACEPTADA")}
              >
                <span className="material-symbols-rounded" aria-hidden="true">
                  close
                </span>
                Sí, el cliente no aceptó
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-primary btn-grande"
                disabled={guardando}
                onClick={() => marcar("ACEPTADA")}
              >
                <span className="material-symbols-rounded" aria-hidden="true">
                  check
                </span>
                Sí, el cliente aceptó
              </button>
            )}
          </div>
        }
      >
        <p className="confirmacion-subtitulo">Al confirmar:</p>
        {confirmando === "ACEPTADA" && (
          <p className="confirmacion-punto">
            <span className="material-symbols-rounded" aria-hidden="true">
              check
            </span>
            Se crea la renta para que Operativo prepare la entrega.
          </p>
        )}
        <p className="confirmacion-punto">
          <span className="material-symbols-rounded" aria-hidden="true">
            check
          </span>
          {confirmando === "NO_ACEPTADA"
            ? "Ya no se podrá enviar ni marcar como aceptada."
            : "Ya no se podrá marcar como no aceptada."}
        </p>
        <p className="confirmacion-punto confirmacion-advertencia">
          <span className="material-symbols-rounded" aria-hidden="true">
            warning
          </span>
          Este paso no se puede deshacer.
        </p>
      </Modal>
    </div>
  );
}

// "¿Qué sigue?" justo después de guardar (HU-17)
function QueSigue({ cotizacion: c }: { cotizacion: Cotizacion }) {
  const pasos = [
    ...(c.estado === "EN_AUTORIZACION"
      ? [{ titulo: "Gerencia autoriza el descuento", texto: "" }]
      : []),
    {
      titulo: "Generar el PDF y mandarlo al cliente",
      texto: "La cotización pasa a «Enviada».",
    },
    {
      titulo: `El cliente acepta antes del ${diaYMes(c.validaHasta)}`,
      texto: `Si no responde en ${c.diasVigencia} días, pasa a «Vencida».`,
    },
  ];

  return (
    <section className="tarjeta">
      <h2>¿Qué sigue?</h2>
      <ol className="que-sigue">
        {pasos.map((p, i) => (
          <li key={p.titulo} className={i === 0 ? "actual" : undefined}>
            <span className="que-sigue-numero">{i + 1}</span>
            <div>
              <strong>{p.titulo}</strong>
              <span className="que-sigue-texto">
                {i === 0 ? "Es el paso en el que está ahora." : p.texto}
              </span>
            </div>
          </li>
        ))}
      </ol>
      <p className="nota-condiciones">
        <span className="material-symbols-rounded" aria-hidden="true">
          info
        </span>
        Las condiciones (anticipo del 30%, cancelación y carta responsiva) ya
        van incluidas.
      </p>
    </section>
  );
}

function VolverACotizaciones() {
  return (
    <Link to="/cotizaciones" className="detalle-volver">
      <span className="material-symbols-rounded" aria-hidden="true">
        arrow_back
      </span>
      Volver a cotizaciones
    </Link>
  );
}

function Dato({
  etiqueta,
  children,
}: {
  etiqueta: string;
  children: ReactNode;
}) {
  return (
    <div className="dato">
      <dt>{etiqueta}</dt>
      <dd>{children}</dd>
    </div>
  );
}