import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useTitulo } from "../../hooks/useTitulo";
import HojaCotizacion from "./HojaCotizacion";
import {
  cambiarEstadoCotizacion,
  obtenerCotizacion,
  obtenerPdf,
  urlPdf,
} from "./cotizacionesApi";
import type { Cotizacion } from "./cotizacionesTypes";
import "../../styles/cotizaciones/cotizaciones.css";
import "../../styles/cotizaciones/pdf-cotizacion.css";

// Descarga un archivo que ya se tiene en memoria
function descargarArchivo(archivo: File) {
  const url = URL.createObjectURL(archivo);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = archivo.name;
  enlace.click();
  // Se libera después: si se libera de inmediato, algunos navegadores cancelan la descarga
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export default function PdfCotizacion() {
  const { id } = useParams();

  const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null);
  const [error, setError] = useState("");

  // El PDF ya descargado, para compartirlo al instante: el celular solo abre
  // el menú de compartir si se llama justo al tocar el botón
  const [archivo, setArchivo] = useState<File | null>(null);

  useTitulo(cotizacion ? `PDF de ${cotizacion.folio}` : "PDF de la cotización");

  useEffect(() => {
    if (!id) return;
    obtenerCotizacion(id)
      .then((c) => {
        setCotizacion(c);
        if (c.partidas.length > 0) {
          obtenerPdf(c.id, `${c.folio}.pdf`)
            .then(setArchivo)
            .catch(() => setError("No fue posible preparar el PDF para compartirlo."));
        }
      })
      .catch(() => setError("No fue posible cargar la cotización."));
  }, [id]);

  // Al descargar o compartir, una cotización en Borrador pasa a Enviada
  async function marcarEnviada() {
    if (!cotizacion || cotizacion.estado !== "BORRADOR") return;
    try {
      setCotizacion(await cambiarEstadoCotizacion(cotizacion.id, "ENVIADA"));
    } catch {
      setError("No se pudo marcar la cotización como Enviada. Intenta de nuevo.");
    }
  }

  async function compartir() {
    if (!archivo || !cotizacion) return;
    setError("");

    if (navigator.canShare?.({ files: [archivo] })) {
      try {
        await navigator.share({ files: [archivo], title: `Cotización ${cotizacion.folio}` });
      } catch (e) {
        // Si cierra el menú sin elegir una app, no se compartió: sigue en Borrador
        if (!(e instanceof DOMException && e.name === "AbortError")) {
          setError("No se pudo compartir el PDF. Intenta descargarlo.");
        }
        return;
      }
    } else {
      // El navegador no permite compartir archivos: se descarga para mandarlo a mano
      descargarArchivo(archivo);
    }
    await marcarEnviada();
  }

  const avisoError = error && (
    <div className="aviso-error" role="alert">
      <span className="material-symbols-rounded">error</span>
      <div>
        <strong>{error}</strong>
      </div>
    </div>
  );

  if (!cotizacion) {
    return (
      <div className="pagina pdf-cotizacion">
        {avisoError || <p>Cargando cotización…</p>}
      </div>
    );
  }

  const sinEquipos = cotizacion.partidas.length === 0;

  return (
    <div className="pagina pdf-cotizacion">
      <Link to={`/cotizaciones/${cotizacion.id}`} className="detalle-volver">
        <span className="material-symbols-rounded" aria-hidden="true">
          arrow_back
        </span>
        Volver a la cotización
      </Link>

      <div className="cotizaciones-titulo">
        <div>
          <h1>PDF de la cotización {cotizacion.folio}</h1>
          <p>
            Revisa el documento antes de mandarlo al cliente. Se puede abrir o
            descargar en laptop y en celular.
          </p>
        </div>
      </div>

      {avisoError}

      <div className="pdf-cuerpo">
        <div className="pdf-visor">
          <HojaCotizacion cotizacion={cotizacion} />
        </div>
        <p className="pdf-nota-celular solo-celular">
          Pellizca la hoja para ampliarla, o ábrela en pantalla completa.
        </p>

        <section className="tarjeta pdf-acciones">
          <h2>Acciones</h2>

          {sinEquipos ? (
            <>
              <button type="button" className="btn btn-primary btn-grande" disabled>
                Descargar PDF
              </button>
              <p className="pdf-nota">Agrega al menos un equipo</p>
            </>
          ) : (
            <>
              <a
                href={urlPdf(cotizacion.id, true)}
                download={`${cotizacion.folio}.pdf`}
                className="btn btn-primary btn-grande"
                onClick={marcarEnviada}
              >
                <span className="material-symbols-rounded" aria-hidden="true">
                  download
                </span>
                Descargar PDF
              </a>
              <a
                href={urlPdf(cotizacion.id)}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-tertiary btn-grande"
              >
                <span className="material-symbols-rounded" aria-hidden="true">
                  open_in_new
                </span>
                Abrir en otra pestaña
              </a>

              <hr className="pdf-divisor" />

              <button
                type="button"
                className="btn btn-primary btn-grande"
                disabled={!archivo}
                onClick={compartir}
              >
                <span className="material-symbols-rounded" aria-hidden="true">
                  share
                </span>
                Compartir PDF
              </button>

              {cotizacion.estado === "BORRADOR" && (
                <p className="pdf-nota">
                  <span className="material-symbols-rounded" aria-hidden="true">
                    lock
                  </span>
                  Al descargarla o compartirla pasa a Enviada y ya no se podrá
                  modificar.
                </p>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}
