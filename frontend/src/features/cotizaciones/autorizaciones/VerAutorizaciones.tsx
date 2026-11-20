import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import Chip from "../../../components/chips";
import FilterRow from "../../../components/filters/filterRow";
import { useTitulo } from "../../../hooks/useTitulo";
import { formatoMoneda } from "../../../utils/formato";
import type { AutorizacionResumen } from "../../cotizaciones/cotizacionesTypes";
import { autorizarDescuento, listarAutorizaciones, rechazarDescuento } from "./autorizacionesApi";
import { chipDeResultado, lineaRespuesta, solicitadaEl, textoDescuento } from "./autorizacionesUtils";
import { DialogoAutorizar, DialogoRechazar } from "./DialogosAutorizacion";
import "../../../styles/cotizaciones/cotizaciones.css";
import "../../../styles/cotizaciones/autorizaciones.css";

// Lo que se acaba de responder, para el aviso de arriba
export interface Respondida {
  resultado: "AUTORIZADA" | "RECHAZADA";
  folio: string;
  solicitante: string;
}

const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`;

export default function AutorizacionesLista() {
  useTitulo("Autorizaciones");
  const estadoNavegacion = useLocation().state as { respondida?: Respondida } | null;

  const [items, setItems] = useState<AutorizacionResumen[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [aviso, setAviso] = useState<Respondida | null>(estadoNavegacion?.respondida ?? null);

  // "PENDIENTE" muestra solo las que esperan respuesta; "all", todas
  const [filtro, setFiltro] = useState("PENDIENTE");

  const [autorizando, setAutorizando] = useState<AutorizacionResumen | null>(null);
  const [rechazando, setRechazando] = useState<AutorizacionResumen | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [errorDialogo, setErrorDialogo] = useState("");

  useEffect(() => {
    listarAutorizaciones()
      .then(setItems)
      .catch(() => setError("No fue posible cargar las autorizaciones."))
      .finally(() => setCargando(false));
  }, []);

  const pendientes = items.filter((a) => a.autorizacion.resultado === "PENDIENTE").length;
  const visibles = filtro === "all" ? items : items.filter((a) => a.autorizacion.resultado === "PENDIENTE");

  const textoConteo = aviso
    ? pendientes === 0
      ? "Ya no tienes pendientes"
      : `Te ${pendientes === 1 ? "queda" : "quedan"} ${plural(pendientes, "pendiente", "pendientes")}`
    : filtro === "all"
      ? `${plural(items.length, "solicitud", "solicitudes")} · ${plural(pendientes, "pendiente", "pendientes")}`
      : plural(pendientes, "pendiente", "pendientes");

  async function responder(
    accion: () => Promise<AutorizacionResumen>,
    resultado: Respondida["resultado"],
  ) {
    setGuardando(true);
    setErrorDialogo("");
    try {
      const actualizada = await accion();
      setItems((anteriores) =>
        anteriores.map((a) => (a.cotizacionId === actualizada.cotizacionId ? actualizada : a)),
      );
      setAviso({ resultado, folio: actualizada.folio, solicitante: actualizada.autorizacion.solicitadaPor });
      setAutorizando(null);
      setRechazando(null);
    } catch (e) {
      setErrorDialogo(e instanceof Error ? e.message : "No se pudo guardar la respuesta. Intenta de nuevo.");
    } finally {
      setGuardando(false);
    }
  }

  const mensajeVacio = cargando ? "Cargando autorizaciones…" : "No hay solicitudes en este filtro.";

  return (
    <div className="pagina autorizaciones">
      {aviso && (
        <div className={aviso.resultado === "AUTORIZADA" ? "aviso-exito" : "aviso-error"} role="status">
          <span className="material-symbols-rounded">
            {aviso.resultado === "AUTORIZADA" ? "check_circle" : "cancel"}
          </span>
          <div>
            <strong>{aviso.resultado === "AUTORIZADA" ? "Descuento autorizado" : "Descuento rechazado"}</strong>
            <p>
              {aviso.resultado === "AUTORIZADA"
                ? `El auxiliar ya puede generar el PDF y enviar la cotización ${aviso.folio} al cliente.`
                : `El auxiliar verá tu comentario. La cotización ${aviso.folio} quedó en «Rechazada».`}
            </p>
          </div>
        </div>
      )}

      <div className="cotizaciones-titulo">
        <div>
          <h1>{filtro === "all" ? "Autorizaciones" : "Autorizaciones pendientes"}</h1>
          <p>
            Cotizaciones con descuento que esperan tu respuesta. No se pueden enviar al cliente hasta que
            respondas.
          </p>
        </div>
      </div>

      <div className="cotizaciones-filtro">
        <span className="cotizaciones-filtro-etiqueta">Mostrar:</span>
        <FilterRow
          options={[{ label: "Pendientes", value: "PENDIENTE", count: pendientes }]}
          selectedValue={filtro}
          allLabel="Todas"
          allCount={items.length}
          onChange={setFiltro}
        />
      </div>

      {error && (
        <div className="aviso-error" role="alert">
          <span className="material-symbols-rounded">error</span>
          <div>
            <strong>{error}</strong>
            <p>Revisa tu conexión y vuelve a abrir la pantalla.</p>
          </div>
        </div>
      )}

      <p className="autorizaciones-conteo">{textoConteo}</p>

      <ul className="autorizaciones-lista">
        {visibles.map((a) => {
          const pendiente = a.autorizacion.resultado === "PENDIENTE";
          return (
            <li key={a.cotizacionId} className={"autorizacion-tarjeta" + (pendiente ? " pendiente" : "")}>
                            <div className="autorizacion-info">
                <div className="autorizacion-encabezado">
                  <h2>Folio {a.folio}</h2>
                  <Chip category="cotizacion" status={chipDeResultado(a.autorizacion.resultado)} />
                </div>

                <dl className="autorizacion-datos">
                  <div>
                    <dt>Cliente</dt>
                    <dd>{[a.cliente, a.empresa].filter(Boolean).join(" · ")}</dd>
                  </div>
                  <div>
                    <dt>Total</dt>
                    <dd>{formatoMoneda(a.total)}</dd>
                  </div>
                  <div className="descuento">
                    <dt>Descuento</dt>
                    <dd>{textoDescuento(a)}</dd>
                  </div>
                  <div>
                    <dt>Solicitada el</dt>
                    <dd>{solicitadaEl(a)}</dd>
                  </div>
                </dl>

                <p className="solo-laptop">{lineaRespuesta(a)}</p>
              </div>

              <div className="autorizacion-acciones">
                {pendiente && (
                  <>
                    <button
                      type="button"
                      className="btn btn-primary btn-grande btn-autorizar"
                      onClick={() => {
                        setErrorDialogo("");
                        setAutorizando(a);
                      }}
                    >
                      <span className="material-symbols-rounded" aria-hidden="true">check</span>
                      Autorizar
                    </button>
                    <button
                      type="button"
                      className="btn btn-tertiary btn-grande btn-rojo"
                      onClick={() => {
                        setErrorDialogo("");
                        setRechazando(a);
                      }}
                    >
                      <span className="material-symbols-rounded" aria-hidden="true">close</span>
                      Rechazar
                    </button>
                  </>
                )}
                <Link to={`/autorizaciones/${a.cotizacionId}`} className="btn btn-tertiary btn-grande">
                  Ver detalle
                </Link>
              </div>
            </li>
          );
        })}
        {visibles.length === 0 && <li className="cotizaciones-vacio">{mensajeVacio}</li>}
      </ul>

      {autorizando && (
        <DialogoAutorizar
          folio={autorizando.folio}
          monto={autorizando.descuentoMonto}
          guardando={guardando}
          error={errorDialogo}
          onConfirmar={() => responder(() => autorizarDescuento(autorizando.cotizacionId), "AUTORIZADA")}
          onCerrar={() => setAutorizando(null)}
        />
      )}
      {rechazando && (
        <DialogoRechazar
          folio={rechazando.folio}
          solicitante={rechazando.autorizacion.solicitadaPor}
          guardando={guardando}
          error={errorDialogo}
          onConfirmar={(comentario) =>
            responder(() => rechazarDescuento(rechazando.cotizacionId, comentario), "RECHAZADA")
          }
          onCerrar={() => setRechazando(null)}
        />
      )}
    </div>
  );
}