import { Link } from "react-router-dom";
import Aviso from "../../components/Aviso";
import Chip from "../../components/chips";
import { urlPdfCarta } from "./cartasApi";
import type { Carta } from "./cartasTypes";
import { ENVIO_PENDIENTE, imprimirPdf, textoVersion } from "./cartasUtils";
import { useGenerarCarta } from "./useCarta";
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
 * Bloque "Carta responsiva" del detalle de la renta (Figma OHU-04): generarla, verla,
 * descargarla, imprimirla y regenerarla. Pensado para ir dentro del detalle de la renta (OHU-03).
 */
export default function CartaResponsiva({ carta, onActualizada }: CartaResponsivaProps) {
  const { generar, pedirRegenerar, generando, error, exito, limpiarMensajes, modales } =
    useGenerarCarta(carta, onActualizada);

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

  return (
    <section
      className={`carta-bloque${firmada ? " carta-bloque-firmada" : enviada ? "" : " carta-bloque-pendiente"}`}
      aria-labelledby="carta-titulo"
    >
      <h2 id="carta-titulo" className="carta-bloque-titulo">
        {icono(firmada ? "task" : enviada ? "outgoing_mail" : "description")}
        {firmada ? "Carta firmada" : enviada ? "Carta enviada" : "Carta generada"}
      </h2>

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

      {!firmada && (
        <p className="carta-destinatario">
          {icono("mail")}
          Se enviará a {carta.correoCliente}
        </p>
      )}

      {mensajes}

      {!firmada && (
        <>
          {/* TODO: enviar por correo cuando se decida con qué cuenta (criterios 6, 8, 13 y 14) */}
          <button
            type="button"
            className="btn btn-primary carta-boton-principal"
            disabled
            aria-describedby="carta-envio-pendiente"
          >
            {icono("send")}
            {enviada ? "Reenviar al cliente" : "Enviar al cliente"}
          </button>
          <p id="carta-envio-pendiente" className="carta-bloque-ayuda">
            {ENVIO_PENDIENTE}
          </p>
        </>
      )}

      <AccionesArchivo carta={carta} conVistaPrevia />

      {carta.puedeGenerar && (
        <div className="carta-enlaces">
          {/* TODO: "Registrar firma" es otra historia (Registrar carta responsiva firmada) */}
          <button type="button" className="carta-enlace" onClick={pedirRegenerar}>
            {icono("autorenew")}
            Regenerar carta
          </button>
        </div>
      )}

      {modales}
    </section>
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
