import type { ReactNode } from "react";
import { Button } from "../../../components/buttons";
import { formatoDireccion, formatoFechaHora, formatoTelefono } from "../../../utils/formato";
import { nombreCompleto, textoDias } from "../cotizacionesUtils";
import { direccionObra } from "./nuevaCotizacionTypes";
import type { BorradorCotizacion } from "./nuevaCotizacionTypes";
import {
  aCentavos,
  calcularDias,
  calcularTotales,
  enPesos,
  fechaHoraIso,
  importeCentavos,
  TASA_IVA_PORCENTAJE,
} from "./calculo";

interface Props {
  borrador: BorradorCotizacion;
  irAPaso: (paso: number) => void;
}

function Seccion({ titulo, paso, irAPaso, children }: { titulo: string; paso: number; irAPaso: Props["irAPaso"]; children: ReactNode }) {
  return (
    <section className="tarjeta">
      <div className="nc-tarjeta-encabezado">
        <h2>{titulo}</h2>
        <Button type="button" variant="tertiary" onClick={() => irAPaso(paso)}>Cambiar</Button>
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

export default function PasoRevisar({ borrador: b, irAPaso }: Props) {
  const dias = calcularDias(b.fechaEntrega, b.fechaRecoleccion) ?? 0;
  const razon = b.cliente?.razonesSociales.find((r) => r.id === b.razonSocialId);
  const obra = direccionObra(b);
  const importes = b.equipos.map((e) => importeCentavos(dias, e.modelo.tarifaDia));
  const { subtotal, iva, total } = calcularTotales(importes);

  return (
    <>
      <div className="aviso-info">
        <span className="material-symbols-rounded" aria-hidden="true">info</span>
        <p>Se guardará como Borrador. Todavía no se envía al cliente. El traslado y el descuento se agregan después.</p>
      </div>

      <div className="detalle-cuerpo">
        <div className="detalle-principal">
          <Seccion titulo="Cliente y entrega" paso={0} irAPaso={irAPaso}>
            <dl className="datos">
              <Dato etiqueta="Cliente">
                {[b.cliente && nombreCompleto(b.cliente), b.cliente?.empresa].filter(Boolean).join(" · ")}
              </Dato>
              <Dato etiqueta="Se factura a">{razon?.nombre} (RFC: {razon?.rfc})</Dato>
            </dl>
          </Seccion>

          <Seccion titulo="Fechas y lugar" paso={1} irAPaso={irAPaso}>
            <dl className="datos">
              <Dato etiqueta="Entrega">{formatoFechaHora(fechaHoraIso(b.fechaEntrega, b.horaEntrega))}</Dato>
              <Dato etiqueta="Recolección">{formatoFechaHora(fechaHoraIso(b.fechaRecoleccion, b.horaRecoleccion))}</Dato>
              <Dato etiqueta="Duración">{textoDias(dias)}</Dato>
              <Dato etiqueta="Lugar de entrega">{formatoDireccion(obra)}</Dato>
              {obra.referencias && <Dato etiqueta="Referencias para llegar">{obra.referencias}</Dato>}
              <Dato etiqueta="Contacto en la obra">{b.contactoObra} · {formatoTelefono(b.telefonoContacto)}</Dato>
            </dl>
          </Seccion>

          <Seccion titulo={`Equipos (${b.equipos.length})`} paso={2} irAPaso={irAPaso}>
            {b.equipos.map((e, i) => (
              <div key={e.id} className="equipo-cotizado">
                <strong>{e.numeroUnidad} · {e.modelo.nombre}</strong>
                <span>{textoDias(dias)} × {enPesos(aCentavos(e.modelo.tarifaDia))} = {enPesos(importes[i])}</span>
              </div>
            ))}
          </Seccion>
        </div>

        <div className="detalle-lateral">
          <section className="tarjeta detalle-totales">
            <h2>Totales</h2>
            <div className="totales-fila">
              <span>Subtotal</span>
              <strong>{enPesos(subtotal)}</strong>
            </div>
            <div className="totales-fila">
              <span>IVA ({TASA_IVA_PORCENTAJE}%)</span>
              <strong>{enPesos(iva)}</strong>
            </div>
            <div className="totales-fila totales-total">
              <span>Total</span>
              <strong>{enPesos(total)}</strong>
            </div>
          </section>
        </div>
      </div>
    </>
  );
}
