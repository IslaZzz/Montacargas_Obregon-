import { useCallback } from "react";
import { Link, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { useTitulo } from "../../hooks/useTitulo";
import { AccionesArchivo } from "./CartaResponsiva";
import { urlPdfCarta } from "./cartasApi";
import type { Carta } from "./cartasTypes";
import { NOMBRE_ESTADO, textoFecha, textoVersion } from "./cartasUtils";
import ChipCarta from "./ChipCarta";
import { useCarta, useEnviarCarta, useGenerarCarta } from "./useCarta";
import "../../styles/cartas/carta-responsiva.css";

// Vista previa de la carta antes de enviarla: /rentas/:id/carta/vista-previa (Figma OHU-04)
export default function VistaPreviaCarta() {
  const { id } = useParams();
  const { carta, setCarta, error } = useCarta(id);
  const actualizar = useCallback((nueva: Carta) => setCarta(nueva), [setCarta]);
  const regenerar = useGenerarCarta(carta, actualizar);
  const envio = useEnviarCarta(carta, actualizar);

  useTitulo(carta ? `Vista previa de la carta ${carta.folio}` : "Vista previa de la carta");

  const volver = (
    <Link to={`/rentas/${id}/carta`} className="carta-volver">
      <span className="material-symbols-rounded" aria-hidden="true">
        arrow_back
      </span>
      Volver a la renta {carta?.folio}
    </Link>
  );

  if (!carta || !carta.tieneArchivo) {
    return (
      <div className="pagina carta-pagina">
        {volver}
        {error && <Aviso tipo="error" titulo={error} />}
        {!carta && !error && <p>Cargando carta…</p>}
        {carta && <Aviso tipo="error" titulo="La renta todavía no tiene una carta generada." />}
      </div>
    );
  }

  const firmada = carta.estado === "FIRMADA";
  const enviada = carta.estado === "ENVIADA";
  const fallo = envio.error !== "";

  return (
    <div className="carta-previa">
      <div className="pagina carta-pagina">
        {volver}
        <h1>Vista previa de la carta.</h1>
        <p className="carta-previa-estado">
          <ChipCarta estado={carta.estado} />
          {firmada ? "La carta ya está firmada." : "Revisa los datos antes de enviarla."}
        </p>

        {regenerar.error && (
          <Aviso tipo="error" titulo={regenerar.error} onCerrar={regenerar.limpiarMensajes} />
        )}
        {regenerar.exito && (
          <Aviso tipo="exito" titulo={regenerar.exito} onCerrar={regenerar.limpiarMensajes} />
        )}
        {envio.exito && <Aviso tipo="exito" titulo={envio.exito} onCerrar={envio.limpiarMensajes} />}

        <div className="carta-visor">
          <div className="carta-visor-barra">
            <span className="material-symbols-rounded" aria-hidden="true">
              picture_as_pdf
            </span>
            {carta.nombreArchivo}
            <a href={urlPdfCarta(carta)} target="_blank" rel="noopener noreferrer" className="carta-visor-abrir">
              Abrir en otra pestaña
              <span className="material-symbols-rounded" aria-hidden="true">
                open_in_new
              </span>
            </a>
          </div>
          {/* key: al regenerarla se vuelve a cargar el PDF nuevo.
              Sin la columna de miniaturas y con la hoja a todo el ancho (es una sola página) */}
          <iframe
            key={carta.version}
            src={`${urlPdfCarta(carta)}#navpanes=0&view=FitH`}
            title={`Carta ${carta.nombreArchivo}`}
          />
        </div>
      </div>

      <aside className="carta-panel" aria-label="Carta responsiva">
        <header className="carta-panel-encabezado">
          <h2>Carta responsiva</h2>
        </header>

        <div className="carta-panel-dato">
          <span>Destinatario</span>
          <strong>{carta.correoEnvio ?? carta.correoCliente}</strong>
          <p>
            {carta.fechaEnvio
              ? `Enviada el ${textoFecha(carta.fechaEnvio)}`
              : "Correo registrado del cliente"}
          </p>
        </div>

        <div className="carta-panel-dato">
          <span>Archivo</span>
          <strong>{carta.nombreArchivo}</strong>
          <p>{textoVersion(carta).replace(" · ", " · Vigente · ")}</p>
        </div>

        <div className="carta-panel-acciones">
          <span>Acciones</span>
          {fallo && (
            <Aviso tipo="error" titulo={envio.error}>
              La carta sigue como {NOMBRE_ESTADO[carta.estado]}. Puedes reintentar el envío.
            </Aviso>
          )}
          {carta.puedeEnviar && (
            <button
              type="button"
              className="btn btn-primary carta-boton-principal"
              onClick={() => {
                regenerar.limpiarMensajes();
                if (fallo) envio.reintentar();
                else envio.pedirEnvio();
              }}
              disabled={envio.enviando}
            >
              <span className="material-symbols-rounded" aria-hidden="true">
                {fallo ? "refresh" : "send"}
              </span>
              {envio.enviando
                ? "Enviando…"
                : fallo
                  ? "Reintentar envío"
                  : enviada
                    ? "Reenviar al cliente"
                    : "Enviar al cliente"}
            </button>
          )}
          <AccionesArchivo carta={carta} />
          {carta.puedeGenerar && (
            <button type="button" className="carta-enlace" onClick={regenerar.pedirRegenerar}>
              <span className="material-symbols-rounded" aria-hidden="true">
                autorenew
              </span>
              Volver a generar carta
            </button>
          )}
        </div>
      </aside>

      {regenerar.modales}
      {envio.modal}
    </div>
  );
}
