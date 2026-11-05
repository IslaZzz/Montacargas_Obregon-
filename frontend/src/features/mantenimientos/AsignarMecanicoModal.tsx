import { useEffect, useState } from "react";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Modal from "../../components/Modal";
import { asignarMantenimientos, listarMecanicos } from "./mantenimientosApi";
import type { Mantenimiento, MecanicoCarga } from "./mantenimientosTypes";
import {
  formatoDiaCorto,
  formatoHora,
  iniciales,
  textoCarga,
} from "./mantenimientosUtils";

interface AsignarMecanicoModalProps {
  // Los mantenimientos a asignar; si está vacío, el modal está cerrado
  mantenimientos: Mantenimiento[];
  onCerrar: () => void;
  // Recibe los mantenimientos ya actualizados y a quién se asignaron
  onAsignado: (actualizados: Mantenimiento[], mecanico: MecanicoCarga) => void;
}

export default function AsignarMecanicoModal({
  mantenimientos,
  onCerrar,
  onAsignado,
}: AsignarMecanicoModalProps) {
  const abierto = mantenimientos.length > 0;
  const total = mantenimientos.length;

  // Si se abrió con "Cambiar", el mecánico que tiene ahora
  const actual = total === 1 ? mantenimientos[0].mecanicoId : null;

  const [mecanicos, setMecanicos] = useState<MecanicoCarga[]>([]);
  const [cargando, setCargando] = useState(false);
  const [elegido, setElegido] = useState<number | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState("");

  // Cada vez que se abre, trae los mecánicos con su carga actual
  useEffect(() => {
    if (!abierto) return;
    setElegido(actual);
    setError("");
    setCargando(true);
    listarMecanicos()
      .then(setMecanicos)
      .catch(() => setError("No fue posible cargar los mecánicos."))
      .finally(() => setCargando(false));
  }, [abierto, actual]);

  async function asignar() {
    if (elegido === null) return;
    setGuardando(true);
    setError("");
    try {
      const actualizados = await asignarMantenimientos({
        mantenimientoIds: mantenimientos.map((m) => m.id),
        personalId: elegido,
      });
      onAsignado(actualizados, mecanicos.find((m) => m.id === elegido)!);
    } catch (err) {
      // Muestra el motivo que manda el backend, por ejemplo si ya está terminado
      setError(err instanceof Error ? err.message : "No fue posible asignar.");
    } finally {
      setGuardando(false);
    }
  }

  const equipos = `${total} ${total === 1 ? "equipo" : "equipos"}`;
  const titulo =
    actual !== null
      ? `Cambiar el mecánico de ${mantenimientos[0].numeroUnidad}.`
      : `Asignar ${equipos} a un mecánico.`;
  const textoBoton = actual !== null ? "Cambiar mecánico" : `Asignar ${equipos}`;

  return (
    <Modal
      abierto={abierto}
      titulo={titulo}
      subtitulo="Elige quién revisará y reparará las fallas reportadas."
      onCerrar={onCerrar}
      acciones={
        <>
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
            onClick={asignar}
            // Sin elegir, o eligiendo al mismo de antes, no hay nada que guardar
            disabled={elegido === null || elegido === actual || guardando}
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              person_add
            </span>
            {guardando ? "Asignando…" : textoBoton}
          </Button>
        </>
      }
    >
      {error && <Aviso tipo="error" titulo={error} />}

      {/* Equipos que se van a asignar */}
      <section>
        <h3 className="asignar-subtitulo">
          {total === 1 ? "Equipo seleccionado" : "Equipos seleccionados"}
        </h3>
        <ul className="asignar-equipos">
          {mantenimientos.map((m) => (
            <li key={m.id}>
              <strong>{m.numeroUnidad}</strong>
              <span className="asignar-falla">
                <span className="material-symbols-rounded" aria-hidden="true">
                  warning
                </span>
                {m.descripcionFalla}
              </span>
              <span className="asignar-salida">
                {m.fechaLimite
                  ? `Sale ${formatoDiaCorto(m.fechaLimite).toLowerCase()} · ${formatoHora(m.fechaLimite)}`
                  : "Sin fecha de salida"}
              </span>
            </li>
          ))}
        </ul>
      </section>

      {/* Lista de mecánicos para elegir uno */}
      <fieldset className="asignar-mecanicos">
        <legend className="asignar-subtitulo">Mecánico</legend>

        {cargando && <span className="asignar-mensaje">Cargando mecánicos…</span>}
        {!cargando && !error && mecanicos.length === 0 && (
          <span className="asignar-mensaje">No hay mecánicos activos registrados.</span>
        )}

        {mecanicos.map((mec) => (
          <label
            key={mec.id}
            className={"asignar-mecanico" + (elegido === mec.id ? " elegido" : "")}
          >
            <input
              type="radio"
              name="mecanico"
              checked={elegido === mec.id}
              onChange={() => setElegido(mec.id)}
            />
            <span className="avatar" aria-hidden="true">
              {iniciales(mec.nombre)}
            </span>
            <span className="asignar-mecanico-texto">
              <strong>{mec.nombre}</strong>
              <span>
                {mec.id === actual ? "Asignado actualmente" : textoCarga(mec.equiposActivos)}
              </span>
            </span>
            {elegido === mec.id && (
              <span className="asignar-elegido">
                <span className="material-symbols-rounded" aria-hidden="true">
                  check
                </span>
                Seleccionado
              </span>
            )}
          </label>
        ))}
      </fieldset>
    </Modal>
  );
}