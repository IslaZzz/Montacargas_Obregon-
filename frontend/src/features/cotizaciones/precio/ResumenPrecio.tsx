import { formatoMoneda } from "../../../utils/formato";
import { textoDias } from "../cotizacionesUtils";
import { calcularPrecio } from "./precioCalculo";

// Cómo va el descuento en el resumen (HU-15)
export type DescuentoResumen =
  | { tipo: "pendiente" } // Antes del paso 3
  | { tipo: "sin" } // Eligió «Sin descuento»
  | { tipo: "invalido" } // Eligió descuento pero el valor no es válido
  | { tipo: "aplicado"; etiqueta: string; monto: number };

interface Props {
  dias: number;
  subtotalEquipos: number;
  trasladoEntrega: number;
  trasladoRecoleccion: number;
  // False en el paso 1 mientras no se haya capturado nada: se muestra "Se captura en el paso 2"
  trasladoCapturado: boolean;
  descuento: DescuentoResumen;
}

export default function ResumenPrecio({
  dias,
  subtotalEquipos,
  trasladoEntrega,
  trasladoRecoleccion,
  trasladoCapturado,
  descuento,
}: Props) {
  // Con un valor inválido el resumen calcula como si no hubiera descuento
  const { subtotal, conDescuento, iva, total, anticipo } = calcularPrecio(
    subtotalEquipos,
    trasladoEntrega,
    trasladoRecoleccion,
    descuento.tipo === "aplicado" ? descuento.monto : 0,
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

      {/* Descuento (HU-15, criterio 3) */}
      <div className="totales-fila">
        <span>{descuento.tipo === "aplicado" ? descuento.etiqueta : "Descuento"}</span>
        {descuento.tipo === "aplicado" ? (
          <strong>−{formatoMoneda(descuento.monto)}</strong>
        ) : (
          <span className="precio-secundario">
            {descuento.tipo === "pendiente"
              ? "Se elige en el paso 3"
              : descuento.tipo === "sin"
                ? "Sin descuento"
                : "—"}
          </span>
        )}
      </div>
      {descuento.tipo === "aplicado" && (
        <div className="totales-fila">
          <strong>Total con descuento</strong>
          <strong>{formatoMoneda(conDescuento)}</strong>
        </div>
      )}

      <div className="totales-fila">
        <span>IVA (16%)</span>
        <strong>{formatoMoneda(iva)}</strong>
      </div>

      <div className="precio-total">
        <span>{descuento.tipo === "pendiente" ? "Total hasta ahora" : "Total"}</span>
        <strong>{formatoMoneda(total)}</strong>
      </div>

      <div className="totales-fila precio-anticipo">
        <span>Anticipo para confirmar (30%)</span>
        <strong>{formatoMoneda(anticipo)}</strong>
      </div>
    </section>
  );
}