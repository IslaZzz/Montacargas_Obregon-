import { useState } from "react";
import { claveDia } from "./dashboardUtils";

const MESES = ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"];
const DIAS_SEMANA = ["Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do"];

interface CalendarioOperacionesProps {
  hoy: Date;
  diasConOperacion: Set<string>; // Claves "2026-10-08" de los días con entrega o recolección
}

// Semanas completas, de lunes a domingo, que cubren el mes
function semanasDelMes(anio: number, mes: number): Date[][] {
  const primero = new Date(anio, mes, 1);
  const ultimo = new Date(anio, mes + 1, 0);
  const lunesAntes = (primero.getDay() + 6) % 7; // Días del mes anterior en la primera semana
  const diasDespues = (7 - ultimo.getDay()) % 7; // Días del mes siguiente en la última semana
  const total = lunesAntes + ultimo.getDate() + diasDespues;

  const dias = Array.from(
    { length: total },
    (_, i) => new Date(anio, mes, 1 - lunesAntes + i),
  );
  return Array.from({ length: total / 7 }, (_, s) => dias.slice(s * 7, s * 7 + 7));
}

// Calendario del mes con los días que tienen operaciones marcados en naranja
export default function CalendarioOperaciones({
  hoy,
  diasConOperacion,
}: CalendarioOperacionesProps) {
  const [visible, setVisible] = useState({
    anio: hoy.getFullYear(),
    mes: hoy.getMonth(),
  });

  const moverMes = (cambio: number) =>
    setVisible(({ anio, mes }) => {
      const fecha = new Date(anio, mes + cambio, 1);
      return { anio: fecha.getFullYear(), mes: fecha.getMonth() };
    });

  // Un año antes y uno después de hoy, más el que se esté viendo
  const primerAnio = Math.min(visible.anio, hoy.getFullYear() - 1);
  const ultimoAnio = Math.max(visible.anio, hoy.getFullYear() + 1);
  const anios = Array.from(
    { length: ultimoAnio - primerAnio + 1 },
    (_, i) => primerAnio + i,
  );

  return (
    <div className="calendario">
      <div className="calendario-barra">
        <button
          type="button"
          className="calendario-flecha"
          aria-label="Mes anterior"
          onClick={() => moverMes(-1)}
        >
          <span className="material-symbols-rounded" aria-hidden="true">
            chevron_left
          </span>
        </button>
        <div className="calendario-selects">
          <select
            aria-label="Mes"
            value={visible.mes}
            onChange={(e) => setVisible({ ...visible, mes: Number(e.target.value) })}
          >
            {MESES.map((nombre, i) => (
              <option key={nombre} value={i}>
                {nombre}
              </option>
            ))}
          </select>
          <select
            aria-label="Año"
            value={visible.anio}
            onChange={(e) => setVisible({ ...visible, anio: Number(e.target.value) })}
          >
            {anios.map((anio) => (
              <option key={anio} value={anio}>
                {anio}
              </option>
            ))}
          </select>
        </div>
        <button
          type="button"
          className="calendario-flecha"
          aria-label="Mes siguiente"
          onClick={() => moverMes(1)}
        >
          <span className="material-symbols-rounded" aria-hidden="true">
            chevron_right
          </span>
        </button>
      </div>

      <table className="calendario-tabla">
        <thead>
          <tr>
            {DIAS_SEMANA.map((dia) => (
              <th key={dia} scope="col">
                {dia}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {semanasDelMes(visible.anio, visible.mes).map((semana) => (
            <tr key={claveDia(semana[0])}>
              {semana.map((dia) => {
                const otroMes = dia.getMonth() !== visible.mes;
                const conOperacion = !otroMes && diasConOperacion.has(claveDia(dia));
                const clase = otroMes ? "otro-mes" : conOperacion ? "con-operacion" : undefined;
                return (
                  <td key={claveDia(dia)} className={clase}>
                    {dia.getDate()}
                    {conOperacion && <span className="solo-lector"> (con operaciones)</span>}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
