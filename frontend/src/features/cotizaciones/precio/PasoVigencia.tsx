import {
  aFecha,
  formatoDireccion,
  formatoFecha,
  formatoFechaHora,
  formatoMoneda,
} from "../../../utils/formato";
import type { Cotizacion } from "../cotizacionesTypes";

// La cancelación es sin costo hasta 3 días antes de la entrega
const DIAS_CANCELACION = 3;

interface Props {
  cotizacion: Cotizacion;
  // Anticipo del 30 % del total con descuento e IVA
  anticipo: number;
}

const fechaLarga = (texto: string) =>
  aFecha(texto).toLocaleDateString("es-MX", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });

const diaConSemana = (fecha: Date) =>
  fecha
    .toLocaleDateString("es-MX", { weekday: "long", day: "numeric", month: "long" })
    .replace(",", "");

// Paso 4 del precio: Vigencia y condiciones (HU-17)
export default function PasoVigencia({ cotizacion: c, anticipo }: Props) {
  const limiteCancelacion = aFecha(c.fechaEntrega);
  limiteCancelacion.setDate(limiteCancelacion.getDate() - DIAS_CANCELACION);

  return (
    <>
      <section className="tarjeta">
        <div className="traslado-encabezado">
          <span className="material-symbols-rounded" aria-hidden="true">
            schedule
          </span>
          <h2>Vigencia</h2>
        </div>

        <div className="vigencia-caja">
          <strong className="vigencia-dias">{c.diasVigencia} días</strong>
          <div>
            <strong>Válida hasta el {fechaLarga(c.validaHasta)}</strong>
            <span>Contando desde el {formatoFecha(c.fechaCreacion)}, día en que se creó.</span>
          </div>
        </div>

        <p className="vigencia-nota">
          <span className="material-symbols-rounded" aria-hidden="true">
            info
          </span>
          Si el cliente no responde en {c.diasVigencia} días, la cotización pasa a «Vencida» y
          hay que hacer otra.
        </p>
      </section>

      <section className="tarjeta">
        <div className="traslado-encabezado">
          <span className="material-symbols-rounded" aria-hidden="true">
            description
          </span>
          <h2>Condiciones de renta</h2>
        </div>

        <ul className="condiciones">
          <Condicion
            icono="percent"
            titulo="Anticipo del 30%"
            texto={`Para confirmar la renta: ${formatoMoneda(anticipo)}.`}
          />
          <Condicion
            icono="event_busy"
            titulo="Política de cancelación"
            texto={`Sin costo hasta ${DIAS_CANCELACION} días antes de la entrega (hasta el ${diaConSemana(limiteCancelacion)}). Con menos tiempo se pierde el anticipo.`}
          />
          <Condicion
            icono="local_shipping"
            titulo="Entrega"
            texto={`${formatoFechaHora(c.fechaEntrega)}, en ${formatoDireccion(c.direccionObra)}.`}
          />
          <Condicion
            icono="assignment"
            titulo="Carta responsiva"
            texto="Se firma al recibir el equipo."
          />
        </ul>

        <p className="vigencia-nota">
          <span className="material-symbols-rounded" aria-hidden="true">
            lock
          </span>
          Las condiciones se cargan solas y no se pueden editar.
        </p>
      </section>
    </>
  );
}

function Condicion({ icono, titulo, texto }: { icono: string; titulo: string; texto: string }) {
  return (
    <li>
      <span className="condicion-icono material-symbols-rounded" aria-hidden="true">
        {icono}
      </span>
      <div>
        <strong>{titulo}</strong>
        <p>{texto}</p>
      </div>
    </li>
  );
}