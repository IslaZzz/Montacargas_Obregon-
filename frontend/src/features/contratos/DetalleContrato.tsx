import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import Chip from "../../components/chips";
import { useTitulo } from "../../hooks/useTitulo";
import {
  aFecha,
  formatoDireccion,
  formatoFecha,
  formatoMoneda,
  formatoTelefono,
} from "../../utils/formato";
import { nombreCompleto, textoDias } from "../cotizaciones/cotizacionesUtils";
import { obtenerContrato } from "./contratosApi";
import type { Contrato } from "./contratosTypes";
import { ESTADOS_CONTRATO } from "./contratosUtils";
import "../../styles/cotizaciones/cotizaciones.css";
import "../../styles/contratos/contratos.css";

// "lunes 12/10/2026, 09:00 a. m."
function diaYHora(texto: string) {
  const fecha = aFecha(texto);
  const dia = fecha.toLocaleDateString("es-MX", { weekday: "long" });
  const horas = fecha.getHours();
  const hora = `${String(horas % 12 || 12).padStart(2, "0")}:${String(fecha.getMinutes()).padStart(2, "0")}`;
  return `${dia} ${formatoFecha(texto)}, ${hora} ${horas < 12 ? "a. m." : "p. m."}`;
}

const conMayuscula = (texto: string) =>
  texto.charAt(0).toUpperCase() + texto.slice(1);

export default function DetalleContrato() {
  const { id } = useParams();
  // generado: se acaba de generar al aceptar la cotización (HU-23)
  const recienGenerado = Boolean(
    (useLocation().state as { generado?: boolean } | null)?.generado,
  );

  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [error, setError] = useState("");

  useTitulo(contrato ? `Contrato ${contrato.folio}` : "Contrato");

  useEffect(() => {
    if (!id) return;
    obtenerContrato(id)
      .then(setContrato)
      .catch(() => setError("No fue posible cargar el contrato."));
  }, [id]);

  if (!contrato) {
    return (
      <div className="pagina detalle-cotizacion">
        <VolverACotizaciones />
        {error ? (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>{error}</strong>
            </div>
          </div>
        ) : (
          <p>Cargando contrato…</p>
        )}
      </div>
    );
  }

  const k = contrato;
  const c = k.cotizacion;
  const empresa = c.cliente.empresa;
  const vencido = k.estado === "APARTADO_VENCIDO";
  const fechaLimite = formatoFecha(k.fechaLimiteAnticipo);

  const subtotalEquipos = c.partidas.reduce((suma, p) => suma + p.importe, 0);
  const traslado = c.trasladoEntrega + c.trasladoRecoleccion;

  return (
    <div className="pagina detalle-cotizacion detalle-contrato">
      <VolverACotizaciones />

      {/* Al aceptar la cotización se abre aquí con este aviso (HU-23) */}
      {recienGenerado && !vencido && (
        <div className="aviso-exito" role="status">
          <span className="material-symbols-rounded">check_circle</span>
          <div>
            <strong>Contrato {k.folio} generado</strong>
            <p>
              La cotización {c.folio} se aceptó. El contrato queda en Apartado
              hasta que se registre el anticipo del 30% (
              {formatoMoneda(k.anticipo)}), a más tardar el {fechaLimite}. Los
              equipos se bloquean al pagar el anticipo.
            </p>
          </div>
        </div>
      )}

      {vencido && (
        <div className="aviso-info" role="status">
          <span className="material-symbols-rounded" aria-hidden="true">
            info
          </span>
          <div>
            <strong>El apartado venció el {fechaLimite}</strong>
            <p>
              No se registró el anticipo en los 3 días. Este contrato ya no se
              puede pagar y los equipos ya no muestran el aviso en otras
              cotizaciones.
            </p>
          </div>
        </div>
      )}

      {/* Título, estado y botones */}
      <div className="detalle-titulo">
        <div className="detalle-titulo-texto">
          <div className="detalle-titulo-folio">
            <h1>
              <span className="solo-laptop">Contrato </span>
              {k.folio}
            </h1>
            <Chip category="contrato" status={ESTADOS_CONTRATO[k.estado].chip} />
          </div>
          <p>
            {nombreCompleto(c.cliente)}
            {empresa && <span className="solo-laptop"> · {empresa}</span>}
            {" · "}Generado el {formatoFecha(k.fechaGeneracion)} desde la
            cotización <Link to={`/cotizaciones/${c.id}`}>{c.folio}</Link>
          </p>
        </div>

        <div className="detalle-titulo-botones">
          <Link
            to={`/contratos/${k.id}/pdf`}
            className="btn btn-primary btn-grande"
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              download
            </span>
            Descargar PDF
          </Link>
        </div>
      </div>

      <div className="detalle-cuerpo">
        {/* Columna izquierda */}
        <div className="detalle-principal">
          <section className="tarjeta detalle-datos">
            <h2>Datos del contrato</h2>
            <dl className="datos">
              <Dato etiqueta="Cliente">
                {[nombreCompleto(c.cliente), empresa].filter(Boolean).join(" · ")}
              </Dato>
              <Dato etiqueta="Se factura a">
                {c.razonSocial.nombre} (RFC: {c.razonSocial.rfc})
              </Dato>
              <Dato etiqueta="Entrega y recolección">
                {conMayuscula(diaYHora(c.fechaEntrega))} →{" "}
                {diaYHora(c.fechaRecoleccion)} ({textoDias(c.dias)})
              </Dato>
              <Dato etiqueta="Estado del apartado">
                {vencido
                  ? `Apartado vencido el ${fechaLimite}: no se registró el anticipo`
                  : `Apartado: si el anticipo no se registra antes del ${fechaLimite}, el apartado vence solo`}
              </Dato>
              <Dato etiqueta="Lugar de entrega">
                {formatoDireccion(c.direccionObra)}
              </Dato>
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
                  {textoDias(c.dias)} × {formatoMoneda(p.tarifaDia)} ={" "}
                  {formatoMoneda(p.importe)}
                </span>
              </div>
            ))}
            <p className="contrato-nota">
              {vencido
                ? "El apartado venció: estos equipos ya no muestran el aviso en otras cotizaciones."
                : "Mientras el contrato está en Apartado, estos equipos siguen disponibles para otras cotizaciones, que muestran el aviso «Este equipo está en otra cotización aceptada». Se bloquean al registrar el anticipo."}
            </p>
          </section>
        </div>

        {/* Columna derecha */}
        <div className="detalle-lateral">
          <section className="tarjeta detalle-totales">
            <h2>Totales</h2>
            <div className="totales-fila">
              <span>Equipos ({textoDias(c.dias)})</span>
              <strong>{formatoMoneda(subtotalEquipos)}</strong>
            </div>
            {traslado > 0 && (
              <div className="totales-fila">
                <span>Traslado (ida y regreso)</span>
                <strong>{formatoMoneda(traslado)}</strong>
              </div>
            )}
            <div className="totales-fila">
              <span>Subtotal</span>
              <strong>{formatoMoneda(c.subtotal)}</strong>
            </div>
            {c.descuentoTipo !== "NINGUNO" && (
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
            <div className="totales-fila">
              <span>Anticipo 30%</span>
              <strong>{formatoMoneda(k.anticipo)}</strong>
            </div>
            <div className="totales-fila">
              <span>Fecha límite de anticipo</span>
              <strong>{vencido ? `Venció el ${fechaLimite}` : fechaLimite}</strong>
            </div>
          </section>
        </div>
      </div>
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
