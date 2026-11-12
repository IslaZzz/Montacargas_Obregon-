import { useState } from "react";
import Avatar from "../../../components/Avatar";
import { Button } from "../../../components/buttons";
import Campo from "../../../components/CampoForm";
import Modal from "../../../components/Modal";
import { ErrorApi } from "../../../services/apiClient";
import { formatoHora } from "../../../utils/fechas";
import { confirmarRecoleccion } from "./detalleApi";
import type { DetalleRenta } from "./detalleTypes";
import { diaDe, diaLargo, diaMes, horaDe, hoyTexto } from "./detalleUtils";

type Vista = "cancelada" | "programada" | "falta" | "confirmada" | "iniciada" | "realizada";

function vistaDe(d: DetalleRenta): Vista {
  if (d.estado === "CANCELADA") return "cancelada";
  if (d.estado === "PROGRAMADA") return "programada";
  if (!d.recoleccionDesde) return "falta";
  if (d.estado === "CERRADA" || d.trasladoRecoleccion?.estado === "COMPLETADO") return "realizada";
  if (d.trasladoRecoleccion?.yaInicio) return "iniciada";
  return "confirmada";
}

interface Props {
  detalle: DetalleRenta;
  onActualizada: (detalle: DetalleRenta, titulo: string, texto: string) => void;
}

/**
 * Bloque de la recolección dentro del detalle de la renta (OHU-12).
 * Se confirma cuando la renta está en curso; se puede cambiar mientras el traslado no inicie.
 */
export default function RecoleccionRenta({ detalle, onActualizada }: Props) {
  const [modalAbierto, setModalAbierto] = useState(false);
  const vista = vistaDe(detalle);
  const responsable = detalle.trasladoRecoleccion?.responsable ?? null;
  const antesDeTiempo = detalle.recoleccionDesde !== null && diaDe(detalle.recoleccionDesde) < detalle.fechaFin;

  function guardada(nuevo: DetalleRenta) {
    const nombre = nuevo.trasladoRecoleccion?.responsable;
    setModalAbierto(false);
    onActualizada(
      nuevo,
      vista === "falta" ? "Recolección confirmada" : "Recolección modificada",
      nombre ? `Ya aparece en la lista de ${nombre}.` : "Su traslado quedó por asignar en Traslados.",
    );
  }

  if (vista === "cancelada") {
    return (
      <section className="recoleccion recoleccion-neutra">
        <h2 className="recoleccion-titulo">
          <span className="material-symbols-rounded" aria-hidden="true">block</span>
          Renta cancelada
        </h2>
        <p>No hay recolección que coordinar.</p>
      </section>
    );
  }

  if (vista === "programada") {
    return (
      <section className="recoleccion recoleccion-neutra">
        <h2 className="recoleccion-titulo">
          <span className="material-symbols-rounded" aria-hidden="true">lock</span>
          Aún no hay recolección
        </h2>
        <p className="recoleccion-texto">Se coordina cuando la renta esté En curso.</p>
        <p className="recoleccion-ayuda">Hoy está Programada: inicia el {diaLargo(detalle.fechaInicio).toLowerCase()}.</p>
        <Button type="button" variant="primary" className="recoleccion-boton" disabled>
          <span className="material-symbols-rounded" aria-hidden="true">lock</span>
          Confirmar fecha y hora
        </Button>
      </section>
    );
  }

  if (vista === "falta") {
    return (
      <section className="recoleccion recoleccion-pendiente">
        <h2 className="recoleccion-titulo">
          <span className="material-symbols-rounded" aria-hidden="true">event_busy</span>
          Falta confirmar la recolección
        </h2>
        <p className="recoleccion-texto">Termina el {diaLargo(detalle.fechaFin).toLowerCase()}.</p>
        <p className="recoleccion-ayuda">Acuerda con el cliente cuándo pasar por los equipos.</p>
        <Button type="button" variant="primary" className="recoleccion-boton" onClick={() => setModalAbierto(true)}>
          <span className="material-symbols-rounded" aria-hidden="true">event_available</span>
          Confirmar fecha y hora
        </Button>
        {modalAbierto && (
          <ModalRecoleccion detalle={detalle} onCerrar={() => setModalAbierto(false)} onGuardada={guardada} />
        )}
      </section>
    );
  }

  // Ya tiene horario: confirmada, en camino o realizada
  const desde = detalle.recoleccionDesde!;
  const hasta = detalle.recoleccionHasta!;
  const titulo =
    vista === "realizada" ? "Equipos recogidos" : vista === "iniciada" ? "Recolección en camino" : "Recolección confirmada";
  const icono = vista === "confirmada" ? "event_available" : vista === "iniciada" ? "local_shipping" : "task_alt";

  return (
    <section className={"recoleccion " + (vista === "confirmada" ? "recoleccion-confirmada" : "recoleccion-neutra")}>
      <h2 className="recoleccion-titulo">
        <span className="material-symbols-rounded" aria-hidden="true">{icono}</span>
        {titulo}
      </h2>
      <p className="recoleccion-dia">{diaLargo(desde)}</p>
      <p className="recoleccion-horario">
        {formatoHora(desde)} – {formatoHora(hasta)}
      </p>
      {antesDeTiempo && vista === "confirmada" && (
        <span className="recoleccion-antes">Terminada antes de tiempo</span>
      )}

      {responsable ? (
        <div className="recoleccion-operador">
          <Avatar nombre={responsable} />
          <span>{responsable}</span>
        </div>
      ) : (
        <p className="recoleccion-ayuda">Todavía no tiene operador. Asígnalo en Traslados.</p>
      )}

      {vista === "confirmada" && (
        <>
          {responsable && <p className="recoleccion-ayuda">La verá en su lista de recolecciones.</p>}
          <Button type="button" variant="secondary" className="recoleccion-boton" onClick={() => setModalAbierto(true)}>
            <span className="material-symbols-rounded" aria-hidden="true">edit_calendar</span>
            Modificar fecha y hora
          </Button>
        </>
      )}
      {vista === "iniciada" && (
        <>
          <p className="recoleccion-ayuda">Va en camino al domicilio del cliente.</p>
          <Button type="button" variant="primary" className="recoleccion-boton" disabled>
            <span className="material-symbols-rounded" aria-hidden="true">lock</span>
            Modificar fecha y hora
          </Button>
          <p className="recoleccion-ayuda">Ya no se puede cambiar: la recolección ya inició.</p>
        </>
      )}
      {vista === "realizada" && <p className="recoleccion-ayuda">Los equipos ya regresaron a la empresa.</p>}

      {modalAbierto && (
        <ModalRecoleccion detalle={detalle} onCerrar={() => setModalAbierto(false)} onGuardada={guardada} />
      )}
    </section>
  );
}

interface ModalProps {
  detalle: DetalleRenta;
  onCerrar: () => void;
  onGuardada: (detalle: DetalleRenta) => void;
}

// Fecha y rango de horas de la recolección
function ModalRecoleccion({ detalle, onCerrar, onGuardada }: ModalProps) {
  const modificar = detalle.recoleccionDesde !== null;
  const hoy = hoyTexto();
  const responsable = detalle.trasladoRecoleccion?.responsable ?? null;

  const [dia, setDia] = useState(
    modificar ? diaDe(detalle.recoleccionDesde!) : detalle.fechaFin >= hoy ? detalle.fechaFin : hoy,
  );
  const [desde, setDesde] = useState(modificar ? horaDe(detalle.recoleccionDesde!) : "08:00");
  const [hasta, setHasta] = useState(modificar ? horaDe(detalle.recoleccionHasta!) : "10:00");
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState("");

  const fechaPasada = dia !== "" && dia < hoy;
  const horasAlReves = desde !== "" && hasta !== "" && hasta <= desde;
  const antesDeTiempo = dia !== "" && !fechaPasada && dia < detalle.fechaFin;
  const valido = dia !== "" && desde !== "" && hasta !== "" && !fechaPasada && !horasAlReves;

  async function guardar() {
    if (!valido) return;
    setGuardando(true);
    setError("");
    try {
      const nuevo = await confirmarRecoleccion(detalle.id, {
        desde: `${dia}T${desde}:00`,
        hasta: `${dia}T${hasta}:00`,
      });
      onGuardada(nuevo);
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : "No fue posible guardar. Revisa tu conexión.");
    } finally {
      setGuardando(false);
    }
  }

  function cerrar() {
    if (!guardando) onCerrar();
  }

  return (
    <Modal
      abierto
      titulo={modificar ? "Modificar recolección" : "Confirmar recolección"}
      subtitulo={`Renta ${detalle.folio} · ${detalle.cliente}`}
      onCerrar={cerrar}
      acciones={
        <>
          <Button type="button" variant="tertiary" onClick={cerrar} disabled={guardando}>
            Cancelar
          </Button>
          <Button type="button" variant="primary" onClick={guardar} disabled={!valido || guardando}>
            <span className="material-symbols-rounded" aria-hidden="true">event_available</span>
            {guardando ? "Guardando…" : modificar ? "Guardar cambio" : "Confirmar recolección"}
          </Button>
        </>
      }
    >
      <div className="recoleccion-modal">
        <Campo
          etiqueta="Fecha"
          type="date"
          min={hoy}
          value={dia}
          error={fechaPasada ? "Esa fecha ya pasó. Elige una a partir de hoy." : undefined}
          onChange={setDia}
        />
        <div className="recoleccion-modal-horas">
          <Campo etiqueta="Desde" type="time" value={desde} onChange={setDesde} />
          <Campo
            etiqueta="Hasta"
            type="time"
            value={hasta}
            error={horasAlReves ? "Debe ser después de la hora de inicio." : undefined}
            onChange={setHasta}
          />
        </div>

        {antesDeTiempo && (
          <div className="recoleccion-modal-aviso" role="status">
            <span className="material-symbols-rounded" aria-hidden="true">warning</span>
            <div>
              <strong>Terminada antes de tiempo</strong>
              <span>Es antes del fin de la renta ({diaMes(detalle.fechaFin)}).</span>
            </div>
          </div>
        )}

        {responsable ? (
          <div className="recoleccion-operador">
            <Avatar nombre={responsable} />
            <span>{responsable}</span>
            <small>{modificar ? "verá el cambio en su lista" : "lo verá en su lista"}</small>
          </div>
        ) : (
          <p className="recoleccion-ayuda">El traslado quedará por asignar en Traslados.</p>
        )}

        {error && (
          <p className="campo-error" role="alert">
            {error}
          </p>
        )}
      </div>
    </Modal>
  );
}