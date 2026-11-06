import { Link } from "react-router-dom";
import Aviso from "../../components/Aviso";
import Chip from "../../components/chips";
import { urlPdfCarta } from "./cartasApi";
import type { Carta } from "./cartasTypes";
import { imprimirPdf, NOMBRE_ESTADO, textoFecha, textoVersion } from "./cartasUtils";
import { useEnviarCarta, useGenerarCarta } from "./useCarta";
import "../../styles/cartas/carta-responsiva.css";

interface CartaResponsivaProps {
  carta: Carta;
  onActualizada: (carta: Carta) => void;
}

const icono = (nombre: string) => (
  <span className="material-symbols-rounded" aria-hidden="true">
    {nombre}
  </span>
);

/**
 * Bloque "Carta responsiva" del detalle de la renta (Figma OHU-04): generarla, verla, enviarla,
 * descargarla, imprimirla y regenerarla. Pensado para ir dentro del detalle de la renta (OHU-03).
 */
export default function CartaResponsiva({ carta, onActualizada }: CartaResponsivaProps) {
  const { generar, pedirRegenerar, generando, error, exito, limpiarMensajes, modales } =
    useGenerarCarta(carta, onActualizada);
  const envio = useEnviarCarta(carta, onActualizada);
  // Al enviar se quitan los avisos de generar, para que no se mezclen con los del envío
  const pedirEnvio = () => {
    limpiarMensajes();
    envio.pedirEnvio();
  };
  const reintentar = () => {
    limpiarMensajes();
    envio.reintentar();
  };

  const mensajes = (
    <>
      {error && (
        <Aviso tipo="error" titulo={error} onCerrar={limpiarMensajes} />
      )}
      {exito && <Aviso tipo="exito" titulo={exito} onCerrar={limpiarMensajes} />}
    </>
  );

  // Sin carta, o una de los datos de prueba que no tiene archivo
  if (carta.estado === "SIN_GENERAR" || !carta.tieneArchivo) {
    const sinArchivo = carta.estado !== "SIN_GENERAR";
    return (
      <section className="carta-bloque carta-bloque-pendiente" aria-labelledby="carta-titulo">
        <h2 id="carta-titulo" className="carta-bloque-titulo">
          {icono("description")}
          {sinArchivo ? "La carta no tiene archivo" : "Falta generar la carta responsiva"}
        </h2>
        <p>Se crea en PDF con los datos del cliente, de los equipos y de la renta.</p>
        <p className="carta-bloque-ayuda">Ya están cargados en la renta.</p>
        {mensajes}
        <button
          type="button"
          className="btn btn-primary carta-boton-principal"
          onClick={generar}
          disabled={!carta.puedeGenerar || generando}
        >
          {icono("note_add")}
          {generando ? "Generando…" : "Generar carta responsiva"}
        </button>
        {!carta.puedeGenerar && (
          <p className="carta-bloque-ayuda">Solo se genera la carta de una renta Programada.</p>
        )}
        {modales}
      </section>
    );
  }

  const enviada = carta.estado === "ENVIADA";
  const firmada = carta.estado === "FIRMADA";
  const cambio = carta.cambios.length > 0;
  const fallo = envio.error !== "";

  const clase = fallo
    ? " carta-bloque-error"
    : firmada
      ? " carta-bloque-firmada"
      : enviada
        ? ""
        : " carta-bloque-pendiente";

  return (
    <section className={`carta-bloque${clase}`} aria-labelledby="carta-titulo">
      <h2 id="carta-titulo" className="carta-bloque-titulo">
        {icono(fallo ? "error" : firmada ? "task" : enviada ? "outgoing_mail" : "description")}
        {fallo
          ? "No se pudo enviar la carta"
          : firmada
            ? "Carta firmada"
            : enviada
              ? "Carta enviada"
              : "Carta generada"}
      </h2>

      {fallo && (
        <Aviso tipo="error" titulo={envio.error}>
          La carta sigue como {NOMBRE_ESTADO[carta.estado]}. Puedes reintentar el envío.
        </Aviso>
      )}

      {cambio && (
        <div className="carta-cambio-aviso" role="status">
          {icono("warning")}
          <div>
            <strong>La renta cambió después de generar la carta.</strong>
            <p>Regenérala para que tenga los datos actuales.</p>
          </div>
        </div>
      )}

      <ArchivoCarta carta={carta} />

      {!firmada && <Destinatario carta={carta} />}

      {mensajes}
      {envio.exito && <Aviso tipo="exito" titulo={envio.exito} onCerrar={envio.limpiarMensajes} />}

      {carta.puedeEnviar && !enviada && (
        <button
          type="button"
          className="btn btn-primary carta-boton-principal"
          onClick={fallo ? reintentar : pedirEnvio}
          disabled={envio.enviando}
        >
          {icono(fallo ? "refresh" : "send")}
          {envio.enviando ? "Enviando…" : fallo ? "Reintentar envío" : "Enviar al cliente"}
        </button>
      )}
      {/* Enviada: el botón principal será "Registrar firma" (otra historia: Registrar carta responsiva firmada) */}
      {enviada && fallo && (
        <button
          type="button"
          className="btn btn-primary carta-boton-principal"
          onClick={reintentar}
          disabled={envio.enviando}
        >
          {icono("refresh")}
          {envio.enviando ? "Enviando…" : "Reintentar envío"}
        </button>
      )}

      <AccionesArchivo carta={carta} conVistaPrevia />

      {carta.puedeGenerar && (
        <div className="carta-enlaces">
          {enviada && carta.puedeEnviar && (
            <button type="button" className="carta-enlace" onClick={pedirEnvio} disabled={envio.enviando}>
              {icono("forward_to_inbox")}
              Reenviar al cliente
            </button>
          )}
          <button type="button" className="carta-enlace" onClick={pedirRegenerar}>
            {icono("autorenew")}
            Regenerar carta
          </button>
        </div>
      )}

      {modales}
      {envio.modal}
    </section>
  );
}

// "Se enviará a…" o, si ya se envió, cuándo y a quién
function Destinatario({ carta }: { carta: Carta }) {
  return (
    <p className="carta-destinatario">
      {icono(carta.fechaEnvio ? "mark_email_read" : "mail")}
      {carta.fechaEnvio
        ? `Enviada el ${textoFecha(carta.fechaEnvio)} a ${carta.correoEnvio}`
        : `Se enviará a ${carta.correoCliente}`}
    </p>
  );
}

// Nombre del archivo, "Vigente" y la versión con su fecha
export function ArchivoCarta({ carta }: { carta: Carta }) {
  return (
    <div className="carta-archivo">
      <span className="carta-archivo-icono material-symbols-rounded" aria-hidden="true">
        picture_as_pdf
      </span>
      <div>
        <p className="carta-archivo-nombre">{carta.nombreArchivo}</p>
        <p className="carta-archivo-version">
          <Chip category="traslado" status="ok">
            Vigente
          </Chip>
          {textoVersion(carta)}
        </p>
      </div>
    </div>
  );
}

interface AccionesArchivoProps {
  carta: Carta;
  conVistaPrevia?: boolean;
}

// Vista previa, Descargar e Imprimir
export function AccionesArchivo({ carta, conVistaPrevia = false }: AccionesArchivoProps) {
  return (
    <div className={`carta-acciones${conVistaPrevia ? " carta-acciones-tres" : ""}`}>
      {conVistaPrevia && (
        <Link to={`/rentas/${carta.rentaId}/carta/vista-previa`} className="btn btn-secondary">
          {icono("visibility")}
          Vista previa
        </Link>
      )}
      <a
        href={urlPdfCarta(carta, true)}
        download={carta.nombreArchivo ?? undefined}
        className="btn btn-secondary"
      >
        {icono("download")}
        Descargar
      </a>
      <button type="button" className="btn btn-secondary" onClick={() => imprimirPdf(urlPdfCarta(carta))}>
        {icono("print")}
        Imprimir
      </button>
    </div>
  );
}
