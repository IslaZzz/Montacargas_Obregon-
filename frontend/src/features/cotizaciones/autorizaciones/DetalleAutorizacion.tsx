import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import Chip from "../../../components/chips";
import { useTitulo } from "../../../hooks/useTitulo";
import {
  formatoDireccion,
  formatoFechaHora,
  formatoMoneda,
  formatoTelefono,
} from "../../../utils/formato";
import { obtenerCotizacion } from "../../cotizaciones/cotizacionesApi";
import type { Cotizacion } from "../../cotizaciones/cotizacionesTypes";
import { chipDeEstado, nombreCompleto, textoDias } from "../../cotizaciones/cotizacionesUtils";
import { autorizarDescuento, rechazarDescuento } from "./autorizacionesApi";
import { lineaLoPidio } from "./autorizacionesUtils";
import { DialogoAutorizar, DialogoRechazar } from "./DialogosAutorizacion";
import type { Respondida } from "./VerAutorizaciones";
import "../../../styles/cotizaciones/cotizaciones.css";
import "../../../styles/cotizaciones/autorizaciones.css";

export default function DetalleAutorizacion() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null);
  const [error, setError] = useState("");

  // Comentario opcional al autorizar
  const [comentario, setComentario] = useState("");
  const [autorizando, setAutorizando] = useState(false);
  const [rechazando, setRechazando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorDialogo, setErrorDialogo] = useState("");

  useTitulo(cotizacion ? `Autorizar descuento de ${cotizacion.folio}` : "Autorizar descuento");

  useEffect(() => {
    if (!id) return;
    obtenerCotizacion(id)
      .then(setCotizacion)
      .catch(() => setError("No fue posible cargar la cotización."));
  }, [id]);

  if (error) {
    return (
      <div className="pagina">
        <div className="aviso-error" role="alert">
          <span className="material-symbols-rounded">error</span>
          <div>
            <strong>{error}</strong>
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
  // Una cotización sin descuento no tiene nada que autorizar
  if (!c.autorizacion) return <Navigate to={`/cotizaciones/${c.id}`} replace />;

  const solicitud = c.autorizacion;
  const pendiente = solicitud.resultado === "PENDIENTE";
  const porcentaje = c.descuentoTipo === "PORCENTAJE" ? ` (${c.descuentoValor}%)` : "";

  async function responder(accion: () => Promise<unknown>, resultado: Respondida["resultado"]) {
    setGuardando(true);
    setErrorDialogo("");
    try {
      await accion();
      const respondida: Respondida = { resultado, folio: c.folio, solicitante: solicitud.solicitadaPor };
      navigate("/autorizaciones", { state: { respondida } });
    } catch (e) {
      setErrorDialogo(e instanceof Error ? e.message : "No se pudo guardar la respuesta. Intenta de nuevo.");
      setGuardando(false);
    }
  }

  const abrirAutorizar = () => {
    setErrorDialogo("");
    setAutorizando(true);
  };
  const abrirRechazar = () => {
    setErrorDialogo("");
    setRechazando(true);
  };

  return (
    <div className="pagina detalle-cotizacion autorizacion-detalle">
      <Link to="/autorizaciones" className="detalle-volver">
        <span className="material-symbols-rounded" aria-hidden="true">arrow_back</span>
        <span className="solo-laptop">Volver a pendientes</span>
        <span className="solo-celular">Volver a autorizaciones</span>
      </Link>

      <div className="detalle-titulo">
        <div className="detalle-titulo-texto">
          <div className="detalle-titulo-folio">
            <h1>
              <span className="solo-laptop">Autorizar descuento · </span>
              {c.folio}
            </h1>
            <Chip category="cotizacion" status={chipDeEstado(c.estado)} />
          </div>
          <p>
            {[nombreCompleto(c.cliente), c.cliente.empresa].filter(Boolean).join(" · ")}.{" "}
            {lineaLoPidio(solicitud.solicitadaPor, solicitud.fechaSolicitud)}
          </p>
        </div>
      </div>

      <div className="detalle-cuerpo">
        <div className="detalle-principal">
          <section className="tarjeta detalle-datos">
            <h2>Cliente y entrega</h2>
            <dl className="datos">
              <Dato etiqueta="Cliente">
                {[nombreCompleto(c.cliente), c.cliente.empresa].filter(Boolean).join(" · ")}
              </Dato>
              <Dato etiqueta="Se factura a">
                {c.razonSocial.nombre} (RFC: {c.razonSocial.rfc})
              </Dato>
              <Dato etiqueta="Entrega">{formatoFechaHora(c.fechaEntrega)}</Dato>
              <Dato etiqueta="Recolección">{formatoFechaHora(c.fechaRecoleccion)}</Dato>
              <Dato etiqueta="Lugar de entrega">{formatoDireccion(c.direccionObra)}</Dato>
              <Dato etiqueta="Contacto en la obra">
                {c.contactoObra} · {formatoTelefono(c.telefonoContacto)}
              </Dato>
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
                  {textoDias(c.dias)} × {formatoMoneda(p.tarifaDia)} = {formatoMoneda(p.importe)}
                </span>
              </div>
            ))}
          </section>
        </div>

        <div className="detalle-lateral">
          <section className="tarjeta detalle-totales">
            <h2>Descuento solicitado</h2>
            <div className="totales-fila">
              <span>Precio sin descuento</span>
              <strong>{formatoMoneda(c.subtotal)}</strong>
            </div>
            <div className="totales-fila">
              <span>Descuento{porcentaje}</span>
              <strong>−{formatoMoneda(c.descuentoMonto)}</strong>
            </div>
            <div className="totales-fila">
              <span>IVA (16%)</span>
              <strong>{formatoMoneda(c.iva)}</strong>
            </div>
            <div className="totales-fila totales-total">
              <span>Total con IVA</span>
              <strong>{formatoMoneda(c.total)}</strong>
            </div>

            <dl className="datos">
              <Dato etiqueta="Motivo del auxiliar">{c.descuentoMotivo}</Dato>
            </dl>

            {pendiente ? (
              <>
                <div className="campo-comentario">
                  <label htmlFor="comentario-autorizar">
                    Comentario para el auxiliar <small>(opcional)</small>
                  </label>
                  <textarea
                    id="comentario-autorizar"
                    value={comentario}
                    maxLength={500}
                    rows={3}
                    placeholder="Ej. Autorizado por ser cliente frecuente"
                    onChange={(e) => setComentario(e.target.value)}
                  />
                </div>

                {/* Laptop: los botones van dentro de la tarjeta; en celular quedan fijos abajo */}
                <div className="autorizacion-botones-tarjeta">
                  <button type="button" className="btn btn-primary btn-grande" onClick={abrirAutorizar}>
                    <span className="material-symbols-rounded" aria-hidden="true">check</span>
                    Autorizar descuento
                  </button>
                  <button type="button" className="btn btn-tertiary btn-grande btn-rojo" onClick={abrirRechazar}>
                    <span className="material-symbols-rounded" aria-hidden="true">close</span>
                    Rechazar descuento
                  </button>
                </div>
              </>
            ) : (
              <dl className="datos">
                <Dato etiqueta="Respuesta de gerencia">
                  {solicitud.resultado === "AUTORIZADA" ? "Autorizada" : "Rechazada"} por {solicitud.respondidaPor}
                  {solicitud.comentario ? `: ${solicitud.comentario}` : ""}
                </Dato>
              </dl>
            )}
          </section>
        </div>
      </div>

      {pendiente && (
        <div className="autorizacion-barra">
          <button type="button" className="btn btn-tertiary btn-grande btn-rojo" onClick={abrirRechazar}>
            <span className="material-symbols-rounded" aria-hidden="true">close</span>
            Rechazar
          </button>
          <button type="button" className="btn btn-primary btn-grande" onClick={abrirAutorizar}>
            <span className="material-symbols-rounded" aria-hidden="true">check</span>
            Autorizar
          </button>
        </div>
      )}

      {autorizando && (
        <DialogoAutorizar
          folio={c.folio}
          monto={c.descuentoMonto}
          guardando={guardando}
          error={errorDialogo}
          onConfirmar={() => responder(() => autorizarDescuento(c.id, comentario), "AUTORIZADA")}
          onCerrar={() => setAutorizando(false)}
        />
      )}
      {rechazando && (
        <DialogoRechazar
          folio={c.folio}
          solicitante={solicitud.solicitadaPor}
          guardando={guardando}
          error={errorDialogo}
          onConfirmar={(texto) => responder(() => rechazarDescuento(c.id, texto), "RECHAZADA")}
          onCerrar={() => setRechazando(false)}
        />
      )}
    </div>
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