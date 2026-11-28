import { formatoMoneda } from "../../../utils/formato";
import type { PartidaCotizacion, TipoDescuento } from "../cotizacionesTypes";
import { textoDias } from "../cotizacionesUtils";
import { limpiarMonto, limpiarPorcentaje } from "./precioCalculo";

export type CampoDescuento = "valor" | "motivo";

export const MAX_MOTIVO = 200;

interface Props {
  // Los equipos con su importe y los días de renta. Al corregir una cotización (HU-32) salen de lo que se está editando
  partidas: Pick<PartidaCotizacion, "id" | "unidad" | "modelo" | "tarifaDia" | "importe">[];
  dias: number;
  tipo: TipoDescuento;
  valores: Record<CampoDescuento, string>;
  errores: Partial<Record<CampoDescuento, string>>;
  // Equipos + traslados, antes del descuento
  subtotal: number;
  // Pesos que se descuentan; null si el valor no es válido
  montoDescuento: number | null;
  elegirTipo: (tipo: TipoDescuento) => void;
  cambiar: (campo: CampoDescuento, valor: string) => void;
}

const OPCIONES: { tipo: TipoDescuento; titulo: string; texto: string }[] = [
  { tipo: "NINGUNO", titulo: "Sin descuento", texto: "Se cobra el precio normal." },
  {
    tipo: "PORCENTAJE",
    titulo: "Porcentaje (%)",
    texto: "Por ejemplo, 10% menos sobre el subtotal.",
  },
  {
    tipo: "MONTO",
    titulo: "Monto",
    texto: "Por ejemplo, $500.00 menos sobre el subtotal.",
  },
];

// Paso 3 del precio: Descuento (HU-15)
export default function PasoDescuento({
  partidas,
  dias,
  tipo,
  valores,
  errores,
  subtotal,
  montoDescuento,
  elegirTipo,
  cambiar,
}: Props) {
  const esPorcentaje = tipo === "PORCENTAJE";

  // Debajo del campo: cuánto se descuenta, o los límites si todavía no hay un valor válido
  const ayuda =
    valores.valor !== "" && montoDescuento !== null
      ? `Son ${formatoMoneda(montoDescuento)} menos sobre el subtotal de ${formatoMoneda(subtotal)}.`
      : esPorcentaje
        ? "Mayor a 0 y menor a 100."
        : `Mayor a $0.00 y menor al subtotal de ${formatoMoneda(subtotal)}.`;

  return (
    <>
      <section className="tarjeta">
        <div className="traslado-encabezado">
          <span className="material-symbols-rounded" aria-hidden="true">
            percent
          </span>
          <h2>¿Lleva descuento?</h2>
        </div>

        {/* Las 3 opciones como tarjetas; por dentro son botones de opción */}
        <fieldset className="descuento-opciones" aria-label="Tipo de descuento">
          {OPCIONES.map((o) => {
            const elegida = o.tipo === tipo;
            return (
              <label
                key={o.tipo}
                className={elegida ? "descuento-opcion elegida" : "descuento-opcion"}
              >
                <span className="descuento-opcion-titulo">
                  <input
                    type="radio"
                    name="tipo-descuento"
                    value={o.tipo}
                    checked={elegida}
                    onChange={() => elegirTipo(o.tipo)}
                  />
                  {o.titulo}
                </span>
                <span className="descuento-opcion-texto">{o.texto}</span>
                {elegida && (
                  <span className="descuento-elegida">
                    <span className="material-symbols-rounded" aria-hidden="true">
                      check
                    </span>
                    Elegida
                  </span>
                )}
              </label>
            );
          })}
        </fieldset>

        {/* El valor y el motivo solo aparecen si hay descuento (criterio 1) */}
        {tipo !== "NINGUNO" && (
          <div className="descuento-campos">
            <div className="traslado-campo">
              <label htmlFor="descuento-valor">
                {esPorcentaje ? "Porcentaje de descuento" : "Monto de descuento"}{" "}
                <span className="nc-secundario">(obligatorio)</span>
              </label>
              <div
                className={
                  errores.valor
                    ? "traslado-caja traslado-caja-error descuento-caja"
                    : "traslado-caja descuento-caja"
                }
              >
                {!esPorcentaje && <span aria-hidden="true">$</span>}
                <input
                  id="descuento-valor"
                  type="text"
                  inputMode="decimal"
                  value={valores.valor}
                  onChange={(e) =>
                    cambiar(
                      "valor",
                      esPorcentaje
                        ? limpiarPorcentaje(e.target.value)
                        : limpiarMonto(e.target.value),
                    )
                  }
                  aria-invalid={errores.valor ? true : undefined}
                />
                {esPorcentaje && <span aria-hidden="true">%</span>}
              </div>
              {errores.valor ? (
                <p className="campo-error" role="alert">
                  {errores.valor}
                </p>
              ) : (
                <p className="nc-secundario">{ayuda}</p>
              )}
            </div>

            <div className="traslado-campo">
              <label htmlFor="descuento-motivo">
                Motivo del descuento <span className="nc-secundario">(obligatorio)</span>
              </label>
              <textarea
                id="descuento-motivo"
                className={errores.motivo ? "descuento-motivo con-error" : "descuento-motivo"}
                rows={3}
                maxLength={MAX_MOTIVO}
                value={valores.motivo}
                onChange={(e) => cambiar("motivo", e.target.value)}
                aria-invalid={errores.motivo ? true : undefined}
              />
              {errores.motivo ? (
                <p className="campo-error" role="alert">
                  {errores.motivo}
                </p>
              ) : (
                <p className="nc-secundario">
                  Gerencia lo verá al autorizar. Máximo {MAX_MOTIVO} caracteres.
                </p>
              )}
            </div>
          </div>
        )}
      </section>

      {/* El precio de cada equipo no cambia con el descuento (criterio 4) */}
      <section className="tarjeta">
        <div className="nc-tarjeta-encabezado descuento-conceptos-encabezado">
          <h2>Precio de cada concepto</h2>
          <span className="descuento-no-cambia">
            <span className="material-symbols-rounded" aria-hidden="true">
              lock
            </span>
            No cambia
          </span>
        </div>
        <ul className="descuento-conceptos">
          {partidas.map((p) => (
            <li key={p.id}>
              <span>
                <strong>
                  {p.unidad} · {p.modelo}
                </strong>
                <small>
                  {textoDias(dias)} × {formatoMoneda(p.tarifaDia)} · tarifa del catálogo
                </small>
              </span>
              <strong>{formatoMoneda(p.importe)}</strong>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}