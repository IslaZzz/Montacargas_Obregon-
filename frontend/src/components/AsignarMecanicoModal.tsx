import { useEffect, useState } from "react";
import Aviso from "./Aviso";
import Avatar from "./Avatar";
import { Button } from "./buttons";
import Modal from "./Modal";
import "../styles/asignar-mecanico.css";

// Un renglón del resumen de lo que se va a asignar
export interface ElementoAsignar {
  id: number;
  titulo: string;       // "MC-014"
  descripcion: string;  // "Fuga de aceite hidráulico" o "Montacargas Toyota 8FGU25"
  detalle?: string;     // A la derecha, por ejemplo "Sale jue 22 oct · 08:00"
  alerta?: boolean;     // Muestra el ícono de advertencia junto a la descripción
}

// Un mecánico para elegir, con cuántos trabajos sin terminar tiene
export interface MecanicoOpcion {
  id: number;
  nombre: string;
  equiposActivos: number;
}

interface AsignarMecanicoModalProps {
  abierto: boolean;
  titulo: string;
  subtitulo?: string;
  elementos: ElementoAsignar[];
  // Si se abrió con "Cambiar", el mecánico que tiene ahora; si no, null
  mecanicoActualId: number | null;
  textoBoton: string;
  // Trae los mecánicos al abrir. Pasa la función del archivo Api, no una flecha nueva
  cargarMecanicos: () => Promise<MecanicoOpcion[]>;
  // Guarda la asignación. Si falla, el mensaje del error se muestra en el modal
  onConfirmar: (mecanico: MecanicoOpcion) => Promise<void>;
  onCerrar: () => void;
}

// 0 → "Sin equipos por atender", 1 → "1 equipo por atender"
const textoCarga = (n: number) =>
  n === 0
    ? "Sin equipos por atender"
    : `${n} ${n === 1 ? "equipo" : "equipos"} por atender`;

export default function AsignarMecanicoModal({
  abierto,
  titulo,
  subtitulo,
  elementos,
  mecanicoActualId,
  textoBoton,
  cargarMecanicos,
  onConfirmar,
  onCerrar,
}: AsignarMecanicoModalProps) {
  const [mecanicos, setMecanicos] = useState<MecanicoOpcion[]>([]);
  const [cargando, setCargando] = useState(false);
  const [elegido, setElegido] = useState<number | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState("");

  // Cada vez que se abre, trae los mecánicos con su carga actual
  useEffect(() => {
    if (!abierto) return;
    setElegido(mecanicoActualId);
    setError("");
    setCargando(true);
    cargarMecanicos()
      .then(setMecanicos)
      .catch(() => setError("No fue posible cargar los mecánicos."))
      .finally(() => setCargando(false));
  }, [abierto, mecanicoActualId, cargarMecanicos]);

  async function confirmar() {
    const mecanico = mecanicos.find((m) => m.id === elegido);
    if (!mecanico) return;
    setGuardando(true);
    setError("");
    try {
      await onConfirmar(mecanico);
    } catch (err) {
      // Muestra el motivo que manda el backend, por ejemplo si ya está terminado
      setError(err instanceof Error ? err.message : "No fue posible asignar.");
    } finally {
      setGuardando(false);
    }
  }

  const total = elementos.length;

  return (
    <Modal
      abierto={abierto}
      titulo={titulo}
      subtitulo={subtitulo}
      onCerrar={onCerrar}
      acciones={
        <>
          <Button type="button" variant="tertiary" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Button>
          <Button
            type="button"
            variant="primary"
            onClick={confirmar}
            // Sin elegir, o eligiendo al mismo de antes, no hay nada que guardar
            disabled={elegido === null || elegido === mecanicoActualId || guardando}
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

      {/* Lo que se va a asignar */}
      <section>
        <h3 className="asignar-subtitulo">
          {total === 1 ? "Equipo seleccionado" : "Equipos seleccionados"}
        </h3>
        <ul className="asignar-equipos">
          {elementos.map((e) => (
            <li key={e.id}>
              <strong>{e.titulo}</strong>
              <span className="asignar-falla">
                {e.alerta && (
                  <span className="material-symbols-rounded" aria-hidden="true">
                    warning
                  </span>
                )}
                {e.descripcion}
              </span>
              <span className="asignar-salida">{e.detalle}</span>
            </li>
          ))}
        </ul>
      </section>

      {/* Mecánicos para elegir uno */}
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
            <Avatar nombre={mec.nombre} />
            <span className="asignar-mecanico-texto">
              <strong>{mec.nombre}</strong>
              <span>
                {mec.id === mecanicoActualId
                  ? "Asignado actualmente"
                  : textoCarga(mec.equiposActivos)}
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