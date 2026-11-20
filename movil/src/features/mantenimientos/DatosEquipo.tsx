import { Dato, ListaDatos } from "../../components/Datos";
import Especificaciones from "../../components/Especificaciones";
import { Tarjeta } from "../../components/Tarjeta";
import { fechaHoraCorta } from "../../utils/fechas";
import type { DetalleMantenimiento } from "./mantenimientosTypes";

/** Código, tipo de equipo, especificaciones del modelo y fecha (límite o de término). Lo usan el detalle y el resumen. */
export default function DatosEquipo({ mantenimiento }: { mantenimiento: DetalleMantenimiento }) {
  const m = mantenimiento;
  return (
    <Tarjeta>
      <ListaDatos>
        <Dato etiqueta="Código">{m.unidad}</Dato>
        <Dato etiqueta="Tipo de equipo">{[m.tipoEquipo, m.modelo].filter(Boolean).join(" · ") || "—"}</Dato>
        <Especificaciones especificaciones={m.especificaciones} />
        {m.fechaFin ? (
          <Dato etiqueta="Terminado">{fechaHoraCorta(m.fechaFin)}</Dato>
        ) : (
          <Dato etiqueta="Fecha límite">{m.fechaLimite ? fechaHoraCorta(m.fechaLimite) : "Sin fecha"}</Dato>
        )}
      </ListaDatos>
    </Tarjeta>
  );
}