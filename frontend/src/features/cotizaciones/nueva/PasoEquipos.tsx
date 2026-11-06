import { useEffect, useMemo, useState } from "react";
import { Button } from "../../../components/buttons";
import { formatoFechaHora } from "../../../utils/formato";
import { textoDias } from "../cotizacionesUtils";
import { listarEquiposOperativos } from "./nuevaCotizacionApi";
import type { BorradorCotizacion, EquipoCatalogo } from "./nuevaCotizacionTypes";
import { aCentavos, calcularDias, calcularTotales, enPesos, fechaHoraIso, importeCentavos } from "./calculo";

interface Props {
  borrador: BorradorCotizacion;
  cambiar: (cambios: Partial<BorradorCotizacion>) => void;
}

const TODAS = "Todas";

function useEquiposOperativos() {
  const [equipos, setEquipos] = useState<EquipoCatalogo[]>([]);
  const [estado, setEstado] = useState<"cargando" | "listo" | "error">("cargando");

  useEffect(() => {
    let vigente = true;
    listarEquiposOperativos()
      .then((lista) => {
        if (!vigente) return;
        setEquipos(lista);
        setEstado("listo");
      })
      .catch(() => vigente && setEstado("error"));
    return () => {
      vigente = false;
    };
  }, []);

  return { equipos, estado };
}

const tarifaPorDia = (e: EquipoCatalogo) => enPesos(aCentavos(e.modelo.tarifaDia));

export default function PasoEquipos({ borrador, cambiar }: Props) {
  const { equipos, estado } = useEquiposOperativos();
  const [categoria, setCategoria] = useState(TODAS);

  const categorias = useMemo(() => [TODAS, ...new Set(equipos.map((e) => e.modelo.categoria))], [equipos]);
  const visibles = categoria === TODAS ? equipos : equipos.filter((e) => e.modelo.categoria === categoria);
  const elegidos = new Set(borrador.equipos.map((e) => e.id));

  const dias = calcularDias(borrador.fechaEntrega, borrador.fechaRecoleccion) ?? 0;
  const importes = borrador.equipos.map((e) => importeCentavos(dias, e.modelo.tarifaDia));
  const { subtotal } = calcularTotales(importes);

  const agregar = (equipo: EquipoCatalogo) => cambiar({ equipos: [...borrador.equipos, equipo] });
  const quitar = (id: number) => cambiar({ equipos: borrador.equipos.filter((e) => e.id !== id) });

  return (
    <div className="secciones">
      <section className="tarjeta nc-disponibles">
        <h2>Equipos disponibles</h2>
        <p>
          Del {formatoFechaHora(fechaHoraIso(borrador.fechaEntrega, borrador.horaEntrega))} al{" "}
          {formatoFechaHora(fechaHoraIso(borrador.fechaRecoleccion, borrador.horaRecoleccion))} ({textoDias(dias)}).
        </p>

        <div className="filters-row" role="group" aria-label="Filtrar por categoría">
          {categorias.map((c) => (
            <button key={c} type="button" aria-pressed={categoria === c}
              className={categoria === c ? "filter-chip active" : "filter-chip"} onClick={() => setCategoria(c)}>
              {c}
            </button>
          ))}
        </div>

        {estado === "cargando" && <p>Cargando equipos…</p>}
        {estado === "error" && <p className="campo-error" role="alert">No se pudieron cargar los equipos.</p>}
        {estado === "listo" && visibles.length === 0 && <p>No hay equipos disponibles en esta categoría.</p>}

        <ul className="nc-lista-equipos">
          {visibles.map((e) => (
            <li key={e.id}>
              <span>
                <strong>{e.numeroUnidad} · {e.modelo.nombre}</strong>
                <span className="nc-secundario">{e.modelo.categoria} · {e.modelo.marca} {e.modelo.codigo}</span>
              </span>
              <span className="nc-tarifa">
                <span className="nc-secundario">Tarifa vigente</span>
                {tarifaPorDia(e)} por día
              </span>
              <Button type="button" variant={elegidos.has(e.id) ? "secondary" : "primary"}
                disabled={elegidos.has(e.id)} onClick={() => agregar(e)}>
                {elegidos.has(e.id) ? "Agregado" : "Agregar"}
              </Button>
            </li>
          ))}
        </ul>
      </section>

      <section className="tarjeta">
        <h2>En esta cotización ({borrador.equipos.length})</h2>
        {borrador.equipos.length === 0 && <p>Todavía no agregas equipos.</p>}
        {borrador.equipos.map((e, i) => (
          <div key={e.id} className="equipo-cotizado nc-equipo-elegido">
            <div>
              <strong>{e.numeroUnidad} · {e.modelo.nombre}</strong>
              <span>{textoDias(dias)} × {tarifaPorDia(e)} = {enPesos(importes[i])}</span>
            </div>
            <Button type="button" variant="tertiary" onClick={() => quitar(e.id)}
              aria-label={`Quitar ${e.numeroUnidad}`}>
              Quitar
            </Button>
          </div>
        ))}
        {borrador.equipos.length > 0 && (
          <div className="totales-fila totales-total">
            <span>Subtotal de equipos</span>
            <strong>{enPesos(subtotal)}</strong>
          </div>
        )}
      </section>
    </div>
  );
}
