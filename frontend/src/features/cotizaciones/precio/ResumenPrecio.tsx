import { formatoMoneda } from "../../../utils/formato";
import { textoDias } from "../cotizacionesUtils";
import { calcularPrecio } from "./precioCalculo";

interface Props {
  dias: number;
  subtotalEquipos: number;
  trasladoEntrega: number;
  trasladoRecoleccion: number;
  // False en el paso 1 mientras no se haya capturado nada: se muestra "Se captura en el paso 2"
  trasladoCapturado: boolean;
}

export default function ResumenPrecio({
  dias,
  subtotalEquipos,
  trasladoEntrega,
  trasladoRecoleccion,
  trasladoCapturado,
}: Props) {
  const { subtotal, iva, total, anticipo } = calcularPrecio(
    subtotalEquipos,
    trasladoEntrega,
    trasladoRecoleccion,
  );

  return (
    <section className="tarjeta precio-resumen">
      <h2>Resumen del precio</h2>

      <div className="totales-fila">
        <span>{trasladoCapturado ? "Subtotal equipos" : `Equipos (${textoDias(dias)})`}</span>
        <strong>{formatoMoneda(subtotalEquipos)}</strong>
      </div>

      {trasladoCapturado ? (
        <>
          <div className="totales-fila">
            <span>Traslado de entrega</span>
            <strong>{formatoMoneda(trasladoEntrega)}</strong>
          </div>
          <div className="totales-fila">
            <span>Traslado de recolección</span>
            <strong>{formatoMoneda(trasladoRecoleccion)}</strong>
          </div>
        </>
      ) : (
        <div className="totales-fila">
          <span>
            Traslado
            <small>Se captura en el paso 2</small>
          </span>
          <span className="precio-secundario">—</span>
        </div>
      )}

      <div className="totales-fila">
        <strong>Subtotal</strong>
        <strong>{formatoMoneda(subtotal)}</strong>
      </div>
      <div className="totales-fila">
        <span>Descuento</span>
        <span className="precio-secundario">Se elige en el paso 3</span>
      </div>
      <div className="totales-fila">
        <span>IVA (16%)</span>
        <strong>{formatoMoneda(iva)}</strong>
      </div>

      <div className="precio-total">
        <span>Total hasta ahora</span>
        <strong>{formatoMoneda(total)}</strong>
      </div>

      <div className="totales-fila precio-anticipo">
        <span>Anticipo para confirmar (30%)</span>
        <strong>{formatoMoneda(anticipo)}</strong>
      </div>
    </section>
  );
}