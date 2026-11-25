import { Link } from "react-router-dom";
import { formatoDiaCorto } from "../../utils/fechas";
import type { RecoleccionProxima } from "./avisosTypes";

const HORA = new Intl.DateTimeFormat("es-MX", { hour: "numeric", minute: "2-digit" });

// "Termina hoy", "Termina mañana", "Termina el jue 26 nov", "Venció hace 2 días"
function textoFin(r: RecoleccionProxima) {
  if (r.vencida) {
    const dias = -r.diasParaFin;
    return `Venció hace ${dias} día${dias === 1 ? "" : "s"}`;
  }
  if (r.diasParaFin === 0) return "Termina hoy";
  if (r.diasParaFin === 1) return "Termina mañana";
  const dia = formatoDiaCorto(r.fechaFin);
  return `Termina el ${dia.charAt(0).toLowerCase()}${dia.slice(1)}`;
}

interface Props {
  recoleccion: RecoleccionProxima;
  nuevo: boolean;
  onIr: () => void; // Cierra la lista
}

// OHU-25: una renta por terminar; desde aquí se programa la recolección
export default function ItemRecoleccionProxima({ recoleccion: r, nuevo, onIr }: Props) {
  const programada = r.recoleccionDesde !== null;
  return (
    <li className={`panel-aviso${nuevo ? " panel-aviso-nuevo" : ""}`}>
      <span className="panel-aviso-icono material-symbols-rounded" aria-hidden="true">
        {r.vencida ? "warning" : "event_repeat"}
      </span>
      <div className="panel-aviso-cuerpo">
        <p className="panel-aviso-titulo">
          {r.vencida ? "Recolección vencida" : "Recolección próxima"}: Renta {r.folio}
          {nuevo && <span className="avisos-oculto"> (nuevo)</span>}
        </p>
        <p>
          {r.cliente}
          {r.equipos.length > 0 && `, ${r.equipos.join(", ")}`}
        </p>
        <p>{r.domicilio}</p>
        <p>
          {textoFin(r)}
          {programada
            ? `, recolección ${r.trasladoFolio} a las ${HORA.format(new Date(r.recoleccionDesde as string))}`
            : ", sin horario de recolección"}
        </p>
        <Link
          to={programada ? "/traslados" : `/rentas/${r.rentaId}`}
          className="panel-aviso-accion"
          onClick={onIr}
        >
          {programada ? "Ver traslados" : "Confirmar fecha y hora"}
          <span className="material-symbols-rounded" aria-hidden="true">
            arrow_forward
          </span>
        </Link>
      </div>
    </li>
  );
}