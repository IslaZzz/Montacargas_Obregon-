import { useState } from "react";
import Modal from "../../../components/Modal";
import { formatoMoneda } from "../../../utils/formato";

interface DialogoAutorizarProps {
  folio: string;
  monto: number; // Pesos que se descuentan
  guardando: boolean;
  error: string;
  onConfirmar: () => void;
  onCerrar: () => void;
}

// «¿Autorizar el descuento de $905.00?»
export function DialogoAutorizar({ folio, monto, guardando, error, onConfirmar, onCerrar }: DialogoAutorizarProps) {
  return (
    <Modal
      abierto
      titulo={`¿Autorizar el descuento de ${formatoMoneda(monto)}?`}
      onCerrar={() => !guardando && onCerrar()}
      acciones={
        <div className="confirmacion-botones">
          <button type="button" className="btn btn-tertiary btn-grande" disabled={guardando} onClick={onCerrar}>
            Cancelar
          </button>
          <button type="button" className="btn btn-primary btn-grande" disabled={guardando} onClick={onConfirmar}>
            <span className="material-symbols-rounded" aria-hidden="true">check</span>
            Sí, autorizar
          </button>
        </div>
      }
    >
      <p>
        La cotización {folio} regresa a «Borrador» con la etiqueta «Autorizada por gerencia». El auxiliar ya podrá
        descargar el PDF y mandarlo al cliente.
      </p>
      {error && <p className="autorizacion-error" role="alert">{error}</p>}
    </Modal>
  );
}

interface DialogoRechazarProps {
  folio: string;
  solicitante: string;
  guardando: boolean;
  error: string;
  onConfirmar: (comentario: string) => void;
  onCerrar: () => void;
}

// «Rechazar el descuento de COT-0158», con el comentario obligatorio
export function DialogoRechazar({ folio, solicitante, guardando, error, onConfirmar, onCerrar }: DialogoRechazarProps) {
  const [comentario, setComentario] = useState("");
  const [intentado, setIntentado] = useState(false);
  const falta = intentado && comentario.trim() === "";

  function confirmar() {
    setIntentado(true);
    if (comentario.trim() !== "") onConfirmar(comentario.trim());
  }

  return (
    <Modal
      abierto
      titulo={`Rechazar el descuento de ${folio}`}
      onCerrar={() => !guardando && onCerrar()}
      acciones={
        <div className="confirmacion-botones">
          <button type="button" className="btn btn-tertiary btn-grande" disabled={guardando} onClick={onCerrar}>
            Cancelar
          </button>
          <button type="button" className="btn btn-peligro btn-grande" disabled={guardando} onClick={confirmar}>
            <span className="material-symbols-rounded" aria-hidden="true">close</span>
            Rechazar descuento
          </button>
        </div>
      }
    >
      <p>El auxiliar verá tu comentario y la cotización quedará en «Rechazada».</p>
      <div className={"campo-comentario" + (falta ? " con-error" : "")}>
        <label htmlFor="comentario-rechazo">Comentario para el auxiliar (obligatorio)</label>
        <textarea
          id="comentario-rechazo"
          value={comentario}
          maxLength={500}
          rows={4}
          placeholder="Ej. El 20% es demasiado; ofrece máximo 10%."
          onChange={(e) => setComentario(e.target.value)}
        />
        {falta && (
          <span className="campo-error" role="alert">
            <span className="material-symbols-rounded" aria-hidden="true">error</span>
            Escribe por qué rechazas el descuento
          </span>
        )}
      </div>
      {error && <p className="autorizacion-error" role="alert">{error}</p>}
    </Modal>
  );
}