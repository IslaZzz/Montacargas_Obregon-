import {
  formatoFecha,
  formatoFechaHora,
  formatoDireccion,
  formatoMoneda,
  formatoTelefono,
} from "../../utils/formato";
import type { Cotizacion } from "./cotizacionesTypes";
import { nombreCompleto, textoDias } from "./cotizacionesUtils";

// "Lunes 12 de..." → "lunes 12 de...", como va dentro de una oración
const enMinuscula = (texto: string) =>
  texto.charAt(0).toLowerCase() + texto.slice(1);

// Vista previa del PDF. Muestra lo mismo que genera el backend (CotizacionPdfService).
export default function HojaCotizacion({ cotizacion: c }: { cotizacion: Cotizacion }) {
  return (
    <article className="hoja-pdf" aria-label={`Vista previa del PDF de ${c.folio}`}>
      <header className="hoja-encabezado">
        <div>
          <p className="hoja-marca">Montacargas Obregón.</p>
          <p className="hoja-pequena">Renta de maquinaria · Cd. Obregón, Sonora</p>
        </div>
        <div className="hoja-folio">
          <p className="hoja-titulo">COTIZACIÓN</p>
          <p className="hoja-numero">Folio {c.folio}</p>
          <p className="hoja-pequena">Fecha: {formatoFecha(c.fechaCreacion)}</p>
        </div>
      </header>

      <div className="hoja-columnas">
        <section>
          <h3>CLIENTE</h3>
          <p>{nombreCompleto(c.cliente)}</p>
          {c.cliente.empresa && <p>{c.cliente.empresa}</p>}
          <p>
            {formatoTelefono(c.cliente.telefono)} · {c.cliente.correo}
          </p>
        </section>
        <section>
          <h3>FACTURAR A</h3>
          <p>{c.razonSocial.nombre}</p>
          <p>RFC: {c.razonSocial.rfc}</p>
          <p>{formatoDireccion(c.razonSocial.direccionFiscal)}</p>
        </section>
      </div>

      <section>
        <h3>ENTREGA Y RECOLECCIÓN</h3>
        <p>Entrega: {enMinuscula(formatoFechaHora(c.fechaEntrega))}</p>
        <p>Recolección: {enMinuscula(formatoFechaHora(c.fechaRecoleccion))}</p>
        <p>Lugar: {formatoDireccion(c.direccionObra)}</p>
        <p>
          Contacto en la obra: {c.contactoObra} · {formatoTelefono(c.telefonoContacto)}
        </p>
      </section>

      <table className="hoja-tabla">
        <thead>
          <tr>
            <th>Unidad</th>
            <th>Modelo</th>
            <th className="hoja-numerico">Días</th>
            <th className="hoja-numerico">Tarifa / día</th>
            <th className="hoja-numerico">Importe</th>
          </tr>
        </thead>
        <tbody>
          {c.partidas.map((p) => (
            <tr key={p.id}>
              <td className="hoja-laptop">{p.unidad}</td>
              <td className="hoja-laptop">{p.modelo}</td>
              <td className="hoja-laptop hoja-numerico">{c.dias}</td>
              <td className="hoja-laptop hoja-numerico">{formatoMoneda(p.tarifaDia)}</td>
              {/* En celular, una línea por equipo como en el prototipo */}
              <td className="hoja-celular">
                {p.unidad} · {p.modelo} · {textoDias(c.dias)}
              </td>
              <td className="hoja-numerico">{formatoMoneda(p.importe)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <dl className="hoja-totales">
        <div>
          <dt>Subtotal</dt>
          <dd>{formatoMoneda(c.subtotal)}</dd>
        </div>
        <div>
          <dt>IVA (16%)</dt>
          <dd>{formatoMoneda(c.iva)}</dd>
        </div>
        <div className="hoja-total">
          <dt>Total</dt>
          <dd>{formatoMoneda(c.total)}</dd>
        </div>
      </dl>
    </article>
  );
}
