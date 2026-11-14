import { Button } from "../../../components/buttons";
import DataTable from "../../../components/table";
import type { TableColumn } from "../../../components/table";
import { formatoMoneda } from "../../../utils/formato";
import type { Cotizacion, PartidaCotizacion } from "../cotizacionesTypes";
import { textoDias } from "../cotizacionesUtils";

interface Props {
  cotizacion: Cotizacion;
  subtotalEquipos: number;
}

// Columnas de la tabla en laptop
function columnasDePrecio(dias: number): TableColumn<PartidaCotizacion>[] {
  return [
    {
      header: "Equipo",
      accessor: (p) => (
        <>
          <span className="celda-cliente">{p.unidad}</span>
          <span className="celda-empresa">{p.modelo}</span>
        </>
      ),
    },
    { header: "Duración", accessor: () => textoDias(dias) },
    {
      header: "Tarifa que se usa",
      accessor: (p) => (
        <>
          <span className="celda-cliente">Por día</span>
          <span className="celda-empresa">{formatoMoneda(p.tarifaDia)} por día</span>
        </>
      ),
    },
    {
      header: "Cálculo",
      accessor: (p) => `${textoDias(dias)} × ${formatoMoneda(p.tarifaDia)}`,
    },
    {
      header: "Importe",
      accessor: (p) => formatoMoneda(p.importe),
      className: "celda-total",
    },
  ];
}

export default function PasoPrecioEquipos({ cotizacion: c, subtotalEquipos }: Props) {
  return (
    <section className="tarjeta precio-equipos">
      <div className="nc-tarjeta-encabezado">
        <h2>Precio de cada equipo</h2>
        {/* El cambio de fechas llega en otra historia; mientras tanto no hace nada */}
        <Button type="button" variant="secondary" disabled>
          <span className="material-symbols-rounded" aria-hidden="true">
            calendar_month
          </span>
          Cambiar fechas
        </Button>
      </div>

      {/* Laptop: tabla */}
      <div className="cotizaciones-tabla">
        <DataTable
          columns={columnasDePrecio(c.dias)}
          data={c.partidas}
          getRowKey={(p) => p.id}
          emptyMessage="Esta cotización no tiene equipos."
        />
      </div>

      {/* Celular: tarjetas */}
      <div className="precio-tarjetas">
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
      </div>

      <div className="totales-fila totales-total">
        <span>Subtotal de equipos</span>
        <strong>{formatoMoneda(subtotalEquipos)}</strong>
      </div>
    </section>
  );
}