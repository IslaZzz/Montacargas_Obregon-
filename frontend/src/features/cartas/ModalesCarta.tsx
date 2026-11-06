import Modal from "../../components/Modal";
import type { Carta, DatosFaltantes } from "./cartasTypes";

interface ModalDatosFaltantesProps {
  faltantes: DatosFaltantes | null;
  onCerrar: () => void;
}

// "No se puede generar la carta": dice qué dato falta y de dónde (Figma OHU-04, datos faltantes)
export function ModalDatosFaltantes({ faltantes, onCerrar }: ModalDatosFaltantesProps) {
  return (
    <Modal
      abierto={faltantes !== null}
      titulo="No se puede generar la carta"
      subtitulo="Faltan datos obligatorios."
      onCerrar={onCerrar}
      acciones={
        // TODO: agregar "Completar datos" cuando existan las pantallas para editar el cliente y la renta
        <button type="button" className="btn btn-primary carta-boton-modal" onClick={onCerrar}>
          Entendido
        </button>
      }
    >
      <ul className="carta-faltantes">
        {Object.entries(faltantes ?? {}).map(([dato, origen]) => (
          <li key={dato}>
            <span className="material-symbols-rounded" aria-hidden="true">
              error
            </span>
            <span className="carta-faltante-dato">{dato}</span>
            <span className="carta-faltante-origen">{origen}</span>
          </li>
        ))}
      </ul>
    </Modal>
  );
}

interface ModalRegenerarProps {
  carta: Carta;
  abierto: boolean;
  generando: boolean;
  onCancelar: () => void;
  onConfirmar: () => void;
}

// Confirma antes de reemplazar la carta: muestra qué cambió y que la versión anterior deja de valer
export function ModalRegenerar({ carta, abierto, generando, onCancelar, onConfirmar }: ModalRegenerarProps) {
  const version = carta.version ?? 1;
  const hayCambios = carta.cambios.length > 0;

  return (
    <Modal
      abierto={abierto}
      titulo="Regenerar carta"
      subtitulo={
        hayCambios
          ? "Los datos de la renta cambiaron después de generarla."
          : "Se creará una versión nueva con los datos actuales de la renta."
      }
      onCerrar={onCancelar}
      acciones={
        <>
          <button type="button" className="btn btn-tertiary carta-boton-modal" onClick={onCancelar}>
            Cancelar
          </button>
          <button
            type="button"
            className="btn btn-primary carta-boton-modal"
            onClick={onConfirmar}
            disabled={generando}
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              autorenew
            </span>
            {generando ? "Regenerando…" : "Regenerar carta"}
          </button>
        </>
      }
    >
      {hayCambios && (
        <div className="carta-cambios">
          <p className="carta-cambios-titulo">Cambios detectados</p>
          <dl>
            {carta.cambios.map((c) => (
              <div key={c.campo} className="carta-cambio">
                <dt>{c.campo}</dt>
                <dd>
                  <span className="carta-cambio-antes">{c.antes || "Sin dato"}</span>
                  <span className="material-symbols-rounded" aria-label="cambia a">
                    arrow_forward
                  </span>
                  <strong>{c.ahora || "Sin dato"}</strong>
                </dd>
              </div>
            ))}
          </dl>
        </div>
      )}

      <p className="carta-versiones">
        <span className="carta-version carta-version-anterior">v{version} · Reemplazada</span>
        <span className="material-symbols-rounded" aria-hidden="true">
          arrow_forward
        </span>
        <span className="carta-version carta-version-vigente">v{version + 1} · Vigente</span>
      </p>

      <p className="carta-modal-nota">
        Solo la versión más reciente queda vigente.
        {carta.estado === "ENVIADA"
          ? ` Tendrás que enviar la v${version + 1}; la etiqueta volverá a Carta: Generada.`
          : ` La v${version} ya no se podrá descargar.`}
      </p>
    </Modal>
  );
}
