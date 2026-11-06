import { useEffect, useState } from "react";
import Avatar from "../../components/Avatar";
import { Button } from "../../components/buttons";
import { ErrorApi } from "../../services/apiClient";
import { asignarResponsable, consultarDisponibilidad } from "./trasladosApi";
import type { OperadorDisponibilidad, Traslado } from "./trasladosTypes";
import {
  formatoDisponibilidadSubtitulo,
  formatoFechaHorarioResumen,
} from "./trasladosUtils";
import "../../styles/traslados/traslados.css";

interface PanelAsignacionProps {
  abierto: boolean;
  traslado: Traslado | null;
  onCerrar: () => void;
  onAsignacionExitosa: (actualizado: Traslado) => void;
}

export default function PanelAsignacion({
  abierto,
  traslado,
  onCerrar,
  onAsignacionExitosa,
}: PanelAsignacionProps) {
  const [operadores, setOperadores] = useState<OperadorDisponibilidad[]>([]);
  const [cargando, setCargando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [seleccionadoId, setSeleccionadoId] = useState<number | null>(null);
  const [errorContextual, setErrorContextual] = useState("");

  const esEdicion = Boolean(traslado?.responsableId);

  useEffect(() => {
    if (!abierto || !traslado) return;

    const controller = new AbortController();

    consultarDisponibilidad(traslado.id)
      .then((lista) => {
        if (!controller.signal.aborted) {
          setOperadores(lista);
          setCargando(false);
        }
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          setErrorContextual(
            err instanceof Error ? err.message : "No fue posible consultar la disponibilidad."
          );
          setCargando(false);
        }
      });

    const alPresionar = (e: KeyboardEvent) => {
      if (e.key === "Escape") onCerrar();
    };
    document.addEventListener("keydown", alPresionar);

    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      controller.abort();
      document.removeEventListener("keydown", alPresionar);
      document.body.style.overflow = prevOverflow;
    };
  }, [abierto, traslado, onCerrar]);

  if (!abierto || !traslado) return null;

  // Derivar el seleccionado inicial del traslado si no se ha elegido otro
  const idActual = seleccionadoId ?? traslado.responsableId;
  const operadorSeleccionado = operadores.find((op) => op.id === idActual);

  async function confirmarAsignacion() {
    if (!traslado || !idActual) return;

    setGuardando(true);
    setErrorContextual("");

    try {
      const actualizado = await asignarResponsable(traslado.id, idActual);
      onAsignacionExitosa(actualizado);
      onCerrar();
    } catch (err) {
      // Manejo de error contextual (ej. empalme de horarios o traslado ya iniciado)
      if (err instanceof ErrorApi) {
        setErrorContextual(err.message);
      } else if (err instanceof Error) {
        setErrorContextual(err.message);
      } else {
        setErrorContextual("Ocurrió un error al asignar el operador.");
      }
    } finally {
      setGuardando(false);
    }
  }

  const textoAccion = esEdicion
    ? `Cambiar a ${operadorSeleccionado?.nombreCompleto ?? ""}`.trim()
    : `Asignar a ${operadorSeleccionado?.nombreCompleto ?? ""}`.trim();

  return (
    <div
      className="panel-lateral-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !guardando) onCerrar();
      }}
      role="dialog"
      aria-modal="true"
    >
      <div className="panel-lateral-drawer">
        {/* Encabezado */}
        <header className="panel-lateral-header">
          <div className="panel-lateral-titulo-bloque">
            <h2>{esEdicion ? "Cambiar responsable" : "Asignar responsable"}</h2>
            <p>Elige a un operador libre en ese horario.</p>
          </div>
          <button
            type="button"
            className="panel-cerrar-btn"
            onClick={onCerrar}
            disabled={guardando}
            aria-label="Cerrar panel"
          >
            <span className="material-symbols-rounded">close</span>
          </button>
        </header>

        {/* Cuerpo */}
        <div className="panel-lateral-body">
          {/* Card Resumen de la Renta y Traslado */}
          <div className="panel-resumen-card">
            <div className="panel-resumen-fila">
              <span className="resumen-col-label">Traslado</span>
              <span className="resumen-col-valor">
                {traslado.folio} · Renta {traslado.rentaFolio}
              </span>
            </div>

            <div className="panel-resumen-fila">
              <span className="resumen-col-label">Tipo</span>
              <span
                className={`badge-tipo ${
                  traslado.tipo === "ENTREGA"
                    ? "badge-tipo-entrega"
                    : "badge-tipo-recoleccion"
                }`}
              >
                <span className="material-symbols-rounded">
                  {traslado.tipo === "ENTREGA" ? "local_shipping" : "swap_horiz"}
                </span>
                {traslado.tipo === "ENTREGA" ? "Entrega" : "Recolección"}
              </span>
            </div>

            <div className="panel-resumen-fila">
              <span className="resumen-col-label">Fecha y horario</span>
              <span className="resumen-col-valor">
                {formatoFechaHorarioResumen(
                  traslado.fechaHoraInicio,
                  traslado.fechaHoraFin
                )}
              </span>
            </div>

            <div className="panel-resumen-fila">
              <span className="resumen-col-label">Cliente</span>
              <span className="resumen-col-valor">{traslado.clienteNombre}</span>
            </div>

            <div className="panel-resumen-fila">
              <span className="resumen-col-label">Domicilio</span>
              <span className="resumen-col-valor">{traslado.direccion}</span>
            </div>
          </div>

          {/* Sección de Operadores */}
          <section className="panel-seccion-operadores">
            <div>
              <h3 className="seccion-op-titulo">Operadores de traslado</h3>
              <p className="seccion-op-subtitulo">
                {formatoDisponibilidadSubtitulo(
                  traslado.fechaHoraInicio,
                  traslado.fechaHoraFin
                )}
              </p>
            </div>

            {/* Alerta de Error Contextual */}
            {errorContextual && (
              <div className="panel-error-contextual" role="alert">
                <span className="material-symbols-rounded">error</span>
                <span>{errorContextual}</span>
              </div>
            )}

            {/* Listado de Operadores */}
            {cargando ? (
              <p style={{ color: "#64748B", fontSize: "14px" }}>
                Consultando disponibilidad de operadores...
              </p>
            ) : (
              <div className="lista-operadores">
                {operadores.map((op) => {
                  const seleccionado = idActual === op.id;

                  return (
                    <div
                      key={op.id}
                      className={`card-operador ${seleccionado ? "seleccionado" : ""}`}
                      onClick={() => {
                        setSeleccionadoId(op.id);
                        setErrorContextual("");
                      }}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          setSeleccionadoId(op.id);
                          setErrorContextual("");
                        }
                      }}
                    >
                      <div className="card-operador-izq">
                        <div className="radio-circulo">
                          {seleccionado && <div className="radio-punto-interno" />}
                        </div>
                        <Avatar nombre={op.nombreCompleto} />
                        <div className="operador-datos">
                          <span className="operador-datos-nombre">
                            {op.nombreCompleto}
                          </span>
                          <span className="operador-datos-motivo">{op.motivo}</span>
                        </div>
                      </div>

                      <div>
                        {op.disponible ? (
                          <span className="badge-disponibilidad badge-disponible">
                            Disponible
                          </span>
                        ) : (
                          <span className="badge-disponibilidad badge-ocupado">
                            <span className="material-symbols-rounded">event_busy</span>
                            Ocupado
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          {/* Aviso contextual informativo */}
          {operadorSeleccionado && (
            <div className="aviso-notificacion-operador">
              <span className="material-symbols-rounded">notifications</span>
              <span>
                {operadorSeleccionado.nombreCompleto} recibirá un aviso con la
                fecha, el horario y el domicilio del traslado.
              </span>
            </div>
          )}
        </div>

        {/* Botones de acción inferiores */}
        <footer className="panel-lateral-footer">
          <Button
            type="button"
            variant="tertiary"
            onClick={onCerrar}
            disabled={guardando}
          >
            Cancelar
          </Button>

          <Button
            type="button"
            variant="primary"
            onClick={confirmarAsignacion}
            disabled={!idActual || guardando}
          >
            <span
              className="material-symbols-rounded"
              style={{ fontSize: "18px", marginRight: "6px" }}
            >
              check
            </span>
            {guardando ? "Guardando..." : textoAccion}
          </Button>
        </footer>
      </div>
    </div>
  );
}
