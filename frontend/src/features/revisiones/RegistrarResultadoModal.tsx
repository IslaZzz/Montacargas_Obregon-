import { useState } from "react";
import { Button } from "../../components/buttons";
import Modal from "../../components/Modal";
import { ErrorApi } from "../../services/apiClient";
import type { ResultadoRevision, Revision, TipoRevision } from "./revisionesTypes";
import { RESULTADOS } from "./revisionesUtils";

interface RegistrarResultadoModalProps {
  revision: Revision;
  tipo: TipoRevision;
  onConfirmar: (resultado: ResultadoRevision, notas: string) => Promise<void>;
  onCerrar: () => void;
}

// Lo que pasa después de guardar, según el resultado y el tipo de revisión
function consecuencia(resultado: ResultadoRevision | null, tipo: TipoRevision) {
  if (resultado === null) return null;
  if (resultado === "REQUIERE_MANTENIMIENTO")
    return "Se creará un mantenimiento por asignar con la falla que describas.";
  return tipo === "SALIDA"
    ? "El equipo quedará listo para entrega. Cuando estén todos los de la renta, se creará su traslado de entrega."
    : "El equipo quedará disponible para otra renta.";
}

/*
 * Captura lo que el mecánico reportó por teléfono o WhatsApp.
 * Se monta cada vez que se abre, así empieza siempre en blanco.
 */
export default function RegistrarResultadoModal({
  revision,
  tipo,
  onConfirmar,
  onCerrar,
}: RegistrarResultadoModalProps) {
  const [resultado, setResultado] = useState<ResultadoRevision | null>(null);
  const [notas, setNotas] = useState("");
  const [enviado, setEnviado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState("");

  const requiereNotas = resultado === "REQUIERE_MANTENIMIENTO";
  const faltaResultado = enviado && resultado === null;
  const faltanNotas = enviado && requiereNotas && !notas.trim();

  async function guardar() {
    setEnviado(true);
    setError("");
    if (resultado === null || (requiereNotas && !notas.trim())) return;

    setGuardando(true);
    try {
      await onConfirmar(resultado, notas.trim());
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : "No fue posible guardar. Revisa tu conexión.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal
      abierto
      titulo={`Resultado de la revisión de ${revision.numeroUnidad}`}
      subtitulo={`${revision.descripcionEquipo} · Revisó ${revision.mecanicoNombre ?? "sin mecánico"}`}
      onCerrar={onCerrar}
      acciones={
        <>
          <Button type="button" variant="tertiary" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Button>
          <Button type="button" variant="primary" onClick={guardar} disabled={guardando}>
            {guardando ? "Guardando…" : "Guardar resultado"}
          </Button>
        </>
      }
    >
      <fieldset className="resultado-opciones">
        <legend className="resultado-etiqueta">¿Cómo salió el equipo?</legend>
        {RESULTADOS.map((r) => (
          <label key={r.valor} className={"resultado-opcion" + (resultado === r.valor ? " elegida" : "")}>
            <input
              type="radio"
              name="resultado"
              value={r.valor}
              checked={resultado === r.valor}
              onChange={() => setResultado(r.valor)}
            />
            <span>
              <strong>{r.etiqueta}</strong>
              {r.descripcion}
            </span>
          </label>
        ))}
        {faltaResultado && (
          <span className="campo-error" role="alert">
            Elige el resultado.
          </span>
        )}
      </fieldset>

      <label className="resultado-notas">
        <span className="resultado-etiqueta">
          {requiereNotas ? "Falla encontrada (obligatorio)" : "Notas (opcional)"}
        </span>
        <textarea
          rows={3}
          maxLength={255}
          value={notas}
          placeholder={requiereNotas ? "Por ejemplo: fuga de aceite en el cilindro de elevación" : ""}
          onChange={(e) => setNotas(e.target.value)}
        />
        {faltanNotas && (
          <span className="campo-error" role="alert">
            Describe la falla para crear el mantenimiento.
          </span>
        )}
      </label>

      {consecuencia(resultado, tipo) && <p className="resultado-consecuencia">{consecuencia(resultado, tipo)}</p>}
      {error && (
        <p className="campo-error" role="alert">
          {error}
        </p>
      )}
    </Modal>
  );
}