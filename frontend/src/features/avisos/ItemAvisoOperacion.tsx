import { Link } from "react-router-dom";
import type { AvisoOperacion, TipoAvisoOperacion } from "./avisosTypes";

const ICONO: Record<TipoAvisoOperacion, string> = {
  TRASLADO_INICIADO: "local_shipping",
  ENTREGA_REGISTRADA: "task_alt",
  RECOLECCION_CONFIRMADA: "local_shipping",
  EQUIPOS_RECIBIDOS: "assignment",
  TAREA_REASIGNADA: "swap_horiz",
};

const HORA = new Intl.DateTimeFormat("es-MX", {
  weekday: "short",
  day: "numeric",
  month: "short",
  hour: "numeric",
  minute: "2-digit",
});

interface Props {
  aviso: AvisoOperacion;
  onIr: () => void; // Lo marca como leído y cierra la lista
}

// Un aviso de los traslados: qué pasó, cuándo y a dónde ir
export default function ItemAvisoOperacion({ aviso, onIr }: Props) {
  const nuevo = !aviso.leido;
  // Los equipos recibidos esperan su revisión de entrada; lo demás se sigue en la renta
  const destino =
    aviso.tipo === "EQUIPOS_RECIBIDOS"
      ? { ruta: "/revisiones", texto: "Asignar revisión" }
      : aviso.rentaId !== null
        ? { ruta: `/rentas/${aviso.rentaId}`, texto: "Ver renta" }
        : null;

  return (
    <li className={`panel-aviso${nuevo ? " panel-aviso-nuevo" : ""}`}>
      <span className="panel-aviso-icono material-symbols-rounded" aria-hidden="true">
        {ICONO[aviso.tipo]}
      </span>
      <div className="panel-aviso-cuerpo">
        <p className="panel-aviso-titulo">
          {aviso.titulo}
          {nuevo && <span className="avisos-oculto"> (nuevo)</span>}
        </p>
        <p>{aviso.detalle}</p>
        <p>{HORA.format(new Date(aviso.fecha))}</p>
        {destino && (
          <Link to={destino.ruta} className="panel-aviso-accion" onClick={onIr}>
            {destino.texto}
            <span className="material-symbols-rounded" aria-hidden="true">
              arrow_forward
            </span>
          </Link>
        )}
      </div>
    </li>
  );
}