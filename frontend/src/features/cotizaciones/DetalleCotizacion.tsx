import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import Chip from "../../components/chips";
import Modal from "../../components/Modal";
import { useTitulo } from "../../hooks/useTitulo";
import {
  formatoFecha,
  formatoFechaHora,
  formatoMoneda,
  formatoDireccion,
  formatoTelefono,
} from "../../utils/formato";
import BotonVerPdf from "./BotonVerPdf";
import { cambiarEstadoCotizacion, obtenerCotizacion } from "./cotizacionesApi";
import type { Cotizacion } from "./cotizacionesTypes";
import { chipDeEstado, ESTADOS_RENTA, nombreCompleto, textoDias } from "./cotizacionesUtils";
import "../../styles/cotizaciones/cotizaciones.css";

type Respuesta = "ACEPTADA" | "NO_ACEPTADA";

// "Enviada el 08/10/2026", según el estado
function textoFechaEstado(c: Cotizacion) {
  const conFecha = (texto: string, fecha: string | null) =>
    fecha ? `${texto} el ${formatoFecha(fecha)}` : texto;

  switch (c.estado) {
    case "BORRADOR":
      return conFecha("Creada", c.fechaCreacion);
    case "ENVIADA":
      return conFecha("Enviada", c.fechaEnvio);
    case "ACEPTADA":
      return conFecha("Aceptada", c.fechaRespuesta);
    case "NO_ACEPTADA":
      return conFecha("No aceptada", c.fechaRespuesta);
  }
}

export default function DetalleCotizacion() {
  const { id } = useParams();
  // Folio de la cotización recién creada en "Nueva cotización" (HU-12), para mostrar el aviso
  const folioCreado = (useLocation().state as { creada?: string } | null)?.creada;

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
      setError("No se pudo guardar la respuesta del cliente. Intenta de nuevo.");
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

  return (
    <div className="pagina detalle-cotizacion">
      <VolverACotizaciones />

      {/* Aviso al llegar desde "Nueva cotización" (HU-12) */}
      {folioCreado && c.estado === "BORRADOR" && (
        <div className="aviso-exito" role="status">
          <span className="material-symbols-rounded">check_circle</span>
          <div>
            <strong>Cotización guardada con el folio {folioCreado}</strong>
            <p>Quedó en Borrador. Revísala y envíala al cliente cuando esté lista.</p>
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
          </div>
          <p>
            {nombreCompleto(c.cliente)}
            {empresa && <span className="solo-laptop"> · {empresa}</span>}
            {" · "}
            {textoFechaEstado(c)}
          </p>
        </div>

        <div className="detalle-titulo-botones">
          {respondida && (
            <Link to="/cotizaciones" className="btn btn-tertiary btn-grande">
              Ir a cotizaciones
            </Link>
          )}
          {/* En Enviada, en celular el botón va debajo de "¿Qué respondió el cliente?" */}
          <BotonVerPdf
            cotizacion={c}
            primario={c.estado === "BORRADOR"}
            className={c.estado === "ENVIADA" ? "solo-laptop" : ""}
          />
        </div>
      </div>

      {c.estado === "ENVIADA" && (
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
          <section className="tarjeta detalle-datos">
            <h2>Cliente y entrega</h2>
            <dl className="datos">
              <Dato etiqueta="Cliente">
                {[nombreCompleto(c.cliente), empresa].filter(Boolean).join(" · ")}
              </Dato>
              <Dato etiqueta="Se factura a">
                {c.razonSocial.nombre} (RFC: {c.razonSocial.rfc})
              </Dato>
              <Dato etiqueta="Entrega">{formatoFechaHora(c.fechaEntrega)}</Dato>
              <Dato etiqueta="Recolección">
                {formatoFechaHora(c.fechaRecoleccion)}
              </Dato>
              <Dato etiqueta="Lugar de entrega">{formatoDireccion(c.direccionObra)}</Dato>
              <Dato etiqueta="Contacto en la obra">
                {c.contactoObra} · {formatoTelefono(c.telefonoContacto)}
              </Dato>
              {c.renta && (
                <Dato etiqueta="Renta">
                  {c.renta.folio} · {ESTADOS_RENTA[c.renta.estado]}
                </Dato>
              )}
            </dl>
          </section>

          <section className="tarjeta detalle-equipos">
            <h2>Equipos ({c.partidas.length})</h2>
            {c.partidas.map((p) => (
              <div key={p.id} className="equipo-cotizado">
                <strong>
                  {p.unidad} · {p.modelo}
                </strong>
                <span>
                  {textoDias(c.dias)} × {formatoMoneda(p.tarifaDia)} ={" "}
                  {formatoMoneda(p.importe)}
                </span>
              </div>
            ))}
          </section>
        </div>

        {/* Columna derecha */}
        <div className="detalle-lateral">
          {c.estado === "ENVIADA" && (
            <section className="tarjeta detalle-respuesta">
              <h2>¿Qué respondió el cliente?</h2>
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
          {c.estado === "ENVIADA" && (
            <div className="detalle-pdf-celular solo-celular">
              <BotonVerPdf cotizacion={c} />
            </div>
          )}

          <section className="tarjeta detalle-totales">
            <h2>Totales</h2>
            <div className="totales-fila">
              <span>Subtotal</span>
              <strong>{formatoMoneda(c.subtotal)}</strong>
            </div>
            <div className="totales-fila">
              <span>IVA (16%)</span>
              <strong>{formatoMoneda(c.iva)}</strong>
            </div>
            <div className="totales-fila totales-total">
              <span>Total</span>
              <strong>{formatoMoneda(c.total)}</strong>
            </div>
          </section>
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
        <p className="confirmacion-punto">
          <span className="material-symbols-rounded" aria-hidden="true">
            check
          </span>
          {confirmando === "NO_ACEPTADA"
            ? "Ya no se podrá enviar ni marcar como aceptada."
            : "Ya no se podrá marcar como no aceptada."}
        </p>
      </Modal>
    </div>
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

function Dato({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return (
    <div className="dato">
      <dt>{etiqueta}</dt>
      <dd>{children}</dd>
    </div>
  );
}
