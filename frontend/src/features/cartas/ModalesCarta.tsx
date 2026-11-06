import { useId, useState } from "react";
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

interface ModalEnviarProps {
  carta: Carta;
  mensajeInicial: string;
  enviando: boolean;
  onCancelar: () => void;
  onEnviar: (mensaje: string) => void;
}

// "Enviar carta al cliente": el correo del cliente no se puede cambiar, el mensaje sí (Figma OHU-04, enviar)
export function ModalEnviar({ carta, mensajeInicial, enviando, onCancelar, onEnviar }: ModalEnviarProps) {
  const [mensaje, setMensaje] = useState(mensajeInicial);
  const idPara = useId();
  const idMensaje = useId();

  return (
    <Modal
      abierto
      titulo="Enviar carta al cliente"
      subtitulo="Recibirá la carta por correo para firmarla electrónicamente."
      onCerrar={onCancelar}
      acciones={
        <>
          <button type="button" className="btn btn-tertiary carta-boton-modal" onClick={onCancelar}>
            Cancelar
          </button>
          <button
            type="button"
            className="btn btn-primary carta-boton-modal"
            onClick={() => onEnviar(mensaje)}
            disabled={enviando}
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              send
            </span>
            {enviando ? "Enviando…" : "Enviar carta"}
          </button>
        </>
      }
    >
      <div className="carta-envio">
        <label htmlFor={idPara}>Para</label>
        <div className="carta-envio-para">
          <input id={idPara} type="email" value={carta.correoCliente} readOnly aria-describedby={`${idPara}-nota`} />
          <span className="material-symbols-rounded" aria-hidden="true">
            lock
          </span>
        </div>
        <p id={`${idPara}-nota`} className="carta-envio-nota">
          Correo registrado del cliente.
        </p>

        <label htmlFor={idMensaje}>Mensaje</label>
        <textarea
          id={idMensaje}
          rows={4}
          maxLength={2000}
          value={mensaje}
          onChange={(e) => setMensaje(e.target.value)}
        />

        <div className="carta-envio-adjunto">
          <span className="material-symbols-rounded" aria-hidden="true">
            picture_as_pdf
          </span>
          {carta.nombreArchivo}
        </div>
      </div>
    </Modal>
  );
}
