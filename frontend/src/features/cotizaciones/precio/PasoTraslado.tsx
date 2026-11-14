import { formatoDireccion } from "../../../utils/formato";
import type { Cotizacion } from "../cotizacionesTypes";
import { limpiarMonto } from "./precioCalculo";


export type CampoTraslado = "entrega" | "recoleccion";

interface Props {
  cotizacion: Cotizacion;
  valores: Record<CampoTraslado, string>;
  errores: Partial<Record<CampoTraslado, string>>;
  cambiar: (campo: CampoTraslado, valor: string) => void;
  formatear: (campo: CampoTraslado) => void;
}

export default function PasoTraslado({ cotizacion: c, valores, errores, cambiar, formatear }: Props) {
  return (
    <section className="tarjeta">
      <div className="traslado-encabezado">
        <span className="material-symbols-rounded" aria-hidden="true">
          local_shipping
        </span>
        <h2>Costo de traslado</h2>
      </div>

      <div className="traslado-lugar">
        <span className="traslado-lugar-etiqueta">
          <span className="material-symbols-rounded" aria-hidden="true">
            location_on
          </span>
          Lugar de entrega
        </span>
        <p>{formatoDireccion(c.direccionObra)}</p>
      </div>

      <div className="traslado-campos">
        <CampoMonto
          id="traslado-entrega"
          etiqueta="Traslado de entrega (MXN)"
          valor={valores.entrega}
          error={errores.entrega}
          alCambiar={(valor) => cambiar("entrega", valor)}
          alSalir={() => formatear("entrega")}
        />
        <CampoMonto
          id="traslado-recoleccion"
          etiqueta="Traslado de recolección (MXN)"
          valor={valores.recoleccion}
          error={errores.recoleccion}
          alCambiar={(valor) => cambiar("recoleccion", valor)}
          alSalir={() => formatear("recoleccion")}
        />
      </div>
    </section>
  );
}

interface CampoMontoProps {
  id: string;
  etiqueta: string;
  valor: string;
  error?: string;
  alCambiar: (valor: string) => void;
  alSalir: () => void;
}

// Campo de dinero con el signo $ adelante
function CampoMonto({ id, etiqueta, valor, error, alCambiar, alSalir }: CampoMontoProps) {
  return (
    <div className="traslado-campo">
      <label htmlFor={id}>
        {etiqueta} <span className="nc-secundario">(opcional)</span>
      </label>
      <div className={error ? "traslado-caja traslado-caja-error" : "traslado-caja"}>
        <span aria-hidden="true">$</span>
        <input
          id={id}
          type="text"
          inputMode="decimal"
          value={valor}
          onChange={(e) => alCambiar(limpiarMonto(e.target.value))}
          onBlur={alSalir}
          aria-invalid={error ? true : undefined}
        />
      </div>
      {error && (
        <p className="campo-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}