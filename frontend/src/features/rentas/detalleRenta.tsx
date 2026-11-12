
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { listarRentas } from "./rentasApi";
import type { Renta } from "./estadosRentaTypes";
import Chip from "../../components/chips";
import { Button } from "../../components/buttons";
import "../../styles/rentas/detalleRenta.css";

type HorarioMode = "exacta" | "rango";

interface RecoleccionLocal {
  desde: string | null;
  hasta: string | null;
}

function formatearFecha(fecha?: string | null) {
  if (!fecha) return "No especificada";

  const valor = fecha.length === 10
    ? new Date(`${fecha}T12:00:00`)
    : new Date(fecha);

  if (Number.isNaN(valor.getTime())) return fecha;

  return new Intl.DateTimeFormat("es-MX", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(valor);
}

function formatearHora(fecha?: string | null) {
  if (!fecha) return "";

  const valor = new Date(fecha);
  if (Number.isNaN(valor.getTime())) return "";

  return new Intl.DateTimeFormat("es-MX", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(valor);
}

function obtenerNombreCliente(renta: Renta) {
  const cliente = renta.cliente;

  if (cliente.empresa) return cliente.empresa;

  return [
    cliente.nombre,
    cliente.apellidoPaterno,
    cliente.apellidoMaterno,
  ].filter(Boolean).join(" ");
}

function obtenerFechaLocal(fecha?: string | null) {
  if (!fecha) return "";
  return fecha.slice(0, 10);
}

function obtenerHoraLocal(fecha?: string | null) {
  if (!fecha) return "";

  const valor = new Date(fecha);
  if (Number.isNaN(valor.getTime())) return "";

  const horas = String(valor.getHours()).padStart(2, "0");
  const minutos = String(valor.getMinutes()).padStart(2, "0");

  return `${horas}:${minutos}`;
}

function crearFechaHora(fecha: string, hora: string) {
  return new Date(`${fecha}T${hora}:00`).toISOString();
}

function fechaDentroDeRenta(
  fecha: string,
  fechaInicio: string,
  fechaFin: string
) {
  return fecha >= fechaInicio && fecha <= fechaFin;
}

export default function DetalleRenta() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [renta, setRenta] = useState<Renta | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalRecoleccion, setModalRecoleccion] = useState(false);
  const [modoHorario, setModoHorario] = useState<HorarioMode>("rango");
  const [fechaRecoleccion, setFechaRecoleccion] = useState("");
  const [horaDesde, setHoraDesde] = useState("09:00");
  const [horaHasta, setHoraHasta] = useState("10:00");
  const [errorFormulario, setErrorFormulario] = useState("");

  const [recoleccion, setRecoleccion] = useState<RecoleccionLocal>({
    desde: null,
    hasta: null,
  });

  useEffect(() => {
    async function cargarRenta() {
      try {
        setLoading(true);

        const rentas = await listarRentas();
        const encontrada = rentas.find(
          item => String(item.id) === id
        );

        if (!encontrada) {
          setError("No se encontró la renta solicitada.");
          return;
        }

        setRenta(encontrada);
        setRecoleccion({
          desde: encontrada.recoleccionDesde,
          hasta: encontrada.recoleccionHasta,
        });
      } catch (err) {
        console.error("Error al cargar el detalle de la renta:", err);
        setError("No fue posible cargar la información de la renta.");
      } finally {
        setLoading(false);
      }
    }

    cargarRenta();
  }, [id]);

  function abrirModalRecoleccion() {
    if (recoleccion.desde && recoleccion.hasta) {
      const fechaDesde = obtenerFechaLocal(recoleccion.desde);
      const fechaHasta = obtenerFechaLocal(recoleccion.hasta);

      setFechaRecoleccion(fechaDesde);
      setHoraDesde(obtenerHoraLocal(recoleccion.desde));
      setHoraHasta(obtenerHoraLocal(recoleccion.hasta));

      setModoHorario(
        recoleccion.desde === recoleccion.hasta
          ? "exacta"
          : "rango"
      );

      // Si las fechas son distintas, conservamos el rango en una sola fecha
      // para este formulario, que trabaja con una fecha común.
      if (fechaDesde !== fechaHasta) {
        setFechaRecoleccion(fechaDesde);
      }
    } else {
      setFechaRecoleccion(renta?.fechaFin ?? "");
      setHoraDesde("09:00");
      setHoraHasta("10:00");
      setModoHorario("rango");
    }

    setErrorFormulario("");
    setModalRecoleccion(true);
  }

  function guardarRecoleccion() {
    if (!fechaRecoleccion || !horaDesde) {
      setErrorFormulario("Completa la fecha y la hora de recolección.");
      return;
    }
    if (
      fechaRecoleccion < renta.fechaInicio ||
      fechaRecoleccion > renta.fechaFin
    ) {
      setErrorFormulario(
        `La fecha debe estar entre ${formatearFecha(renta.fechaInicio)} y ${formatearFecha(renta.fechaFin)}.`
      );
      return;
    }
    if (
      !fechaDentroDeRenta(
        fechaRecoleccion,
        renta.fechaInicio,
        renta.fechaFin
      )
    ) {
      setErrorFormulario(
        `La fecha debe estar entre ${formatearFecha(renta.fechaInicio)} y ${formatearFecha(renta.fechaFin)}.`
      );
      return;
    }
  }

    if (modoHorario === "rango" && !horaHasta) {
      setErrorFormulario("Indica la hora límite del rango.");
      return;
    }

    if (modoHorario === "rango" && horaDesde >= horaHasta) {
      setErrorFormulario("La hora final debe ser posterior a la hora inicial.");
      return;
    }

    const desde = crearFechaHora(fechaRecoleccion, horaDesde);
    const hasta = modoHorario === "exacta"
      ? desde
      : crearFechaHora(fechaRecoleccion, horaHasta);

    setRecoleccion({ desde, hasta });
    setModalRecoleccion(false);
    setErrorFormulario("");
  }

  if (loading) {
    return <main className="detalle-loading">Cargando renta...</main>;
  }

  if (error || !renta) {
    return (
      <main className="detalle-loading">
        <p>{error ?? "Renta no encontrada."}</p>
        <Button variant="secondary" onClick={() => navigate("/rentas")}>
          Volver a rentas
        </Button>
      </main>
    );
  }

  const cliente = renta.cliente as Renta["cliente"] & {
    correo?: string;
  };

  const nombreCliente = obtenerNombreCliente(renta);

  const estadoChip =
    renta.estado === "PROGRAMADA" ? "programado" :
    renta.estado === "EN_CURSO" ? "en-curso" :
    null;

  const cantidadEquipos = renta.equipos?.length ?? 0;

  const fechaEntrega = renta.entregaDesde
    ? formatearFecha(renta.entregaDesde)
    : formatearFecha(renta.fechaInicio);

  const horaEntrega = renta.entregaDesde && renta.entregaHasta
    ? `${formatearHora(renta.entregaDesde)} – ${formatearHora(renta.entregaHasta)}`
    : "Horario pendiente";

  const fechaFin = formatearFecha(renta.fechaFin);

  const duracionDias = Math.max(
    1,
    Math.ceil(
      (new Date(`${renta.fechaFin}T12:00:00`).getTime() -
        new Date(`${renta.fechaInicio}T12:00:00`).getTime()) /
        (1000 * 60 * 60 * 24)
    )
  );

  const tieneRecoleccion = Boolean(recoleccion.desde && recoleccion.hasta);

  const recoleccionAnticipada = tieneRecoleccion &&
    obtenerFechaLocal(recoleccion.desde) < renta.fechaFin;

  const horarioRecoleccion = recoleccion.desde && recoleccion.hasta
    ? recoleccion.desde === recoleccion.hasta
      ? formatearHora(recoleccion.desde)
      : `${formatearHora(recoleccion.desde)} – ${formatearHora(recoleccion.hasta)}`
    : "";

  return (
    <main className="detalle-renta-page">

      <section className="detalle-renta-main">
        <button
          type="button"
          className="detalle-back"
          onClick={() => navigate("/rentas")}
        >
          <span className="material-symbols-rounded">arrow_back</span>
          Volver a rentas
        </button>

        <h1>Renta #{renta.folio}</h1>

        <div className="detalle-renta-heading">
          {estadoChip ? (
            <Chip category="renta" status={estadoChip} />
          ) : (
            <span className="detalle-status-neutral">{renta.estado}</span>
          )}

          <span className="detalle-letter-status">
            <span className="status-dot" />
            Carta: Sin generar
          </span>

          <span className="detalle-client-name">{nombreCliente}</span>
        </div>

        <section className="carta-alerta">
          <div className="carta-alerta-title">
            <span className="material-symbols-rounded">description</span>
            <h2>Falta generar la carta responsiva</h2>
          </div>

          <p className="carta-alerta-description">
            Se crea en PDF con los datos del cliente, equipo y renta.
          </p>

          <p className="carta-alerta-note">
            Ya están cargados en la renta: no necesitas escribir nada.
          </p>

          <Button
            variant="primary"
            className="carta-generate-button"
            disabled
            title="La generación de PDF todavía no está conectada al backend"
          >
            <span className="material-symbols-rounded">note_add</span>
            Generar carta responsiva
          </Button>
        </section>

        {/* PANEL DE RECOLECCIÓN */}
        <section className={`recoleccion-panel ${tieneRecoleccion ? "recoleccion-confirmada" : "recoleccion-pendiente"}`}>
          <div className="recoleccion-panel-heading">
            <span className="material-symbols-rounded">
              {tieneRecoleccion ? "event_available" : "event_busy"}
            </span>

            <div>
              <h2>
                {tieneRecoleccion
                  ? "Recolección programada"
                  : "Falta confirmar la recolección"}
              </h2>
              <p>
                {tieneRecoleccion
                  ? "La fecha y el horario de recolección están registrados."
                  : `La renta termina el ${fechaFin}. Confirma cuándo se recogerán los equipos.`}
              </p>
            </div>
          </div>

          {tieneRecoleccion ? (
            <div className="recoleccion-schedule">
              <div>
                <small>Fecha de recolección</small>
                <strong>{formatearFecha(recoleccion.desde)}</strong>
              </div>

              <div>
                <small>Horario</small>
                <strong>{horarioRecoleccion}</strong>
              </div>

              {recoleccionAnticipada && (
                <span className="recoleccion-early-tag">
                  Terminado antes de tiempo
                </span>
              )}
            </div>
          ) : null}

          <Button
            variant="primary"
            className="recoleccion-action"
            onClick={abrirModalRecoleccion}
            disabled={renta.estado !== "EN_CURSO"}
            title={
              renta.estado !== "EN_CURSO"
                ? "La recolección se programa cuando la renta está en curso."
                : undefined
            }
          >
            <span className="material-symbols-rounded">
              {tieneRecoleccion ? "edit_calendar" : "calendar_add_on"}
            </span>
            {tieneRecoleccion
              ? "Modificar fecha y hora"
              : "Confirmar recolección"}
          </Button>

          {renta.estado !== "EN_CURSO" && (
            <p className="recoleccion-disabled-note">
              La recolección se coordina cuando la renta esté EN CURSO.
            </p>
          )}
        </section>

        <section className="detalle-summary">
          <div className="detalle-summary-item">
            <span className="material-symbols-rounded">forklift</span>
            <div>
              <small>Equipos</small>
              <strong>
                {cantidadEquipos} {cantidadEquipos === 1 ? "equipo" : "equipos"}
              </strong>
            </div>
            <span className="material-symbols-rounded summary-arrow">chevron_right</span>
          </div>

          <div className="detalle-summary-item">
            <span className="material-symbols-rounded">description</span>
            <div>
              <small>Carta responsiva</small>
              <strong>Sin generar</strong>
            </div>
          </div>

          <div className="detalle-summary-item">
            <span className="material-symbols-rounded">local_shipping</span>
            <div>
              <small>Entrega</small>
              <strong>{fechaEntrega}</strong>
            </div>
          </div>
        </section>

        <section className="detalle-equipment-list">
          <h2>Equipos incluidos</h2>

          {renta.equipos?.map(equipo => (
            <div className="detalle-equipment" key={equipo.id}>
              <div>
                <strong>{equipo.numeroUnidad}</strong>
                <small>{equipo.numeroSerie}</small>
              </div>
              <span>{equipo.fase.replaceAll("_", " ")}</span>
            </div>
          ))}
        </section>

        <p className="detalle-permissions">
          <span className="material-symbols-rounded">lock</span>
          Gerencia y auxiliar generan y envían la carta. Auxiliar y
          traslados y operación registran la firma.
        </p>
      </section>

      <aside className="detalle-renta-sidebar">
        <h2>Datos de la renta</h2>

        <section className="detalle-info-block">
          <small>Contacto</small>
          <strong>{renta.contactoObra || nombreCliente}</strong>
          <span>{renta.telefonoContacto || "Teléfono no registrado"}</span>
        </section>

        <section className="detalle-info-block">
          <small>Correo electrónico</small>
          <strong>{cliente.correo || "Correo no registrado"}</strong>
        </section>

        <section className="detalle-info-block">
          <small>Domicilio de entrega</small>
          <strong>{renta.direccionObra || "Domicilio no registrado"}</strong>
        </section>

        <section className="detalle-info-block">
          <small>Periodo de renta</small>
          <strong>
            {formatearFecha(renta.fechaInicio)} – {fechaFin}
          </strong>
          <span>{duracionDias} días</span>
        </section>

        <section className="detalle-info-block">
          <small>Entrega</small>
          <strong>{fechaEntrega}</strong>
          <span>{horaEntrega}</span>
        </section>

        <section className="detalle-info-block">
          <small>Recolección</small>
          <strong>
            {recoleccion.desde
              ? formatearFecha(recoleccion.desde)
              : "Pendiente de programar"}
          </strong>
          <span>{horarioRecoleccion}</span>
        </section>
      </aside>

      {/* MODAL DE RECOLECCIÓN */}
      {modalRecoleccion && (
        <div
          className="recoleccion-modal-overlay"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setModalRecoleccion(false);
            }
          }}
        >
          <section
            className="recoleccion-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="recoleccion-modal-title"
          >
            <div className="recoleccion-modal-header">
              <div>
                <small>{tieneRecoleccion ? "MODIFICAR PROGRAMACIÓN" : "NUEVA PROGRAMACIÓN"}</small>
                <h2 id="recoleccion-modal-title">
                  {tieneRecoleccion ? "Modificar recolección" : "Confirmar recolección"}
                </h2>
              </div>

              <button
                type="button"
                className="recoleccion-modal-close"
                aria-label="Cerrar"
                onClick={() => setModalRecoleccion(false)}
              >
                <span className="material-symbols-rounded">close</span>
              </button>
            </div>

            <div className="recoleccion-modal-body">
              <label className="recoleccion-field">
                Fecha de recolección
                <input
                  type="date"
                  value={fechaRecoleccion}
                  min={renta.fechaInicio}
                  max={renta.fechaFin}
                  onChange={(event) => {
                    setFechaRecoleccion(event.target.value);
                    setErrorFormulario("");
                  }}
                />
              </label>

              <div className="recoleccion-mode-tabs">
                <button
                  type="button"
                  className={modoHorario === "exacta" ? "active" : ""}
                  onClick={() => setModoHorario("exacta")}
                >
                  Hora exacta
                </button>

                <button
                  type="button"
                  className={modoHorario === "rango" ? "active" : ""}
                  onClick={() => setModoHorario("rango")}
                >
                  Rango de horario
                </button>
              </div>

              {modoHorario === "exacta" ? (
                <label className="recoleccion-field">
                  Hora
                  <input
                    type="time"
                    value={horaDesde}
                    onChange={(event) => setHoraDesde(event.target.value)}
                  />
                </label>
              ) : (
                <div className="recoleccion-time-range">
                  <label className="recoleccion-field">
                    Desde
                    <input
                      type="time"
                      value={horaDesde}
                      onChange={(event) => setHoraDesde(event.target.value)}
                    />
                  </label>

                  <label className="recoleccion-field">
                    Hasta
                    <input
                      type="time"
                      value={horaHasta}
                      onChange={(event) => setHoraHasta(event.target.value)}
                    />
                  </label>
                </div>
              )}

              {fechaRecoleccion && fechaRecoleccion < renta.fechaFin && (
                <div className="recoleccion-early-warning">
                  <span className="material-symbols-rounded">warning</span>
                  <p>
                    Esta fecha es anterior al fin del periodo de renta.
                    La recolección se registrará como terminación anticipada.
                  </p>
                </div>
              )}

              {errorFormulario && (
                <p className="recoleccion-form-error">{errorFormulario}</p>
              )}
            </div>

            <div className="recoleccion-modal-actions">
              <button
                type="button"
                className="recoleccion-cancel-button"
                onClick={() => setModalRecoleccion(false)}
              >
                Cancelar
              </button>

              <Button variant="primary" onClick={guardarRecoleccion}>
                {tieneRecoleccion ? "Guardar cambios" : "Confirmar recolección"}
              </Button>
            </div>
          </section>
        </div>
      )}
    </main>
  );
}
