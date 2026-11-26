import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useTitulo } from "../../hooks/useTitulo";
import { formatoFecha, formatoMoneda } from "../../utils/formato";
import HojaCotizacion from "../cotizaciones/HojaCotizacion";
import { obtenerContrato, urlPdfContrato } from "./contratosApi";
import type { Contrato } from "./contratosTypes";
import { POLITICA, TITULO_POLITICA } from "./contratosUtils";
import "../../styles/cotizaciones/cotizaciones.css";
import "../../styles/cotizaciones/pdf-cotizacion.css";
import "../../styles/contratos/contratos.css";

// Vista previa y descarga del PDF del contrato (HU-23)
export default function PdfContrato() {
  const { id } = useParams();

  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [error, setError] = useState("");

  useTitulo(contrato ? `PDF de ${contrato.folio}` : "PDF del contrato");

  useEffect(() => {
    if (!id) return;
    obtenerContrato(id)
      .then(setContrato)
      .catch(() => setError("No fue posible cargar el contrato."));
  }, [id]);

  if (!contrato) {
    return (
      <div className="pagina pdf-cotizacion">
        {error ? (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>{error}</strong>
            </div>
          </div>
        ) : (
          <p>Cargando contrato…</p>
        )}
      </div>
    );
  }

  const k = contrato;

  return (
    <div className="pagina pdf-cotizacion">
      <Link to={`/contratos/${k.id}`} className="detalle-volver">
        <span className="material-symbols-rounded" aria-hidden="true">
          arrow_back
        </span>
        Volver al contrato
      </Link>

      <div className="cotizaciones-titulo">
        <div>
          <h1>
            PDF <span className="solo-laptop">del contrato </span>
            {k.folio}
          </h1>
          <p>
            Así lo recibe el cliente. Se puede abrir o descargar en laptop y en
            celular.
          </p>
        </div>
      </div>

      <div className="pdf-cuerpo">
        <div className="pdf-visor">
          {/* La misma hoja que la cotización, con el anticipo y la política del contrato (ContratoPdfService) */}
          <HojaCotizacion
            cotizacion={k.cotizacion}
            documento={{ titulo: "CONTRATO", folio: k.folio, fecha: k.fechaGeneracion }}
          >
            <p className="hoja-anticipo">
              Anticipo 30%: {formatoMoneda(k.anticipo)}, a pagar antes del{" "}
              {formatoFecha(k.fechaLimiteAnticipo)}. Con el anticipo, el
              contrato pasa a Contratado.
            </p>
            <section className="hoja-condiciones">
              <h3>{TITULO_POLITICA}</h3>
              <ul className="hoja-politica">
                {POLITICA.map((renglon) => (
                  <li key={renglon}>{renglon}</li>
                ))}
              </ul>
            </section>
          </HojaCotizacion>
        </div>
        <p className="pdf-nota-celular solo-celular">
          Pellizca la hoja para ampliarla, o ábrela en pantalla completa.
        </p>

        <section className="tarjeta pdf-acciones">
          <h2>Acciones</h2>
          <a
            href={urlPdfContrato(k.id, true)}
            download={`${k.folio}.pdf`}
            className="btn btn-primary btn-grande"
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              download
            </span>
            Descargar PDF
          </a>
          <a
            href={urlPdfContrato(k.id)}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-tertiary btn-grande"
          >
            <span className="material-symbols-rounded" aria-hidden="true">
              open_in_new
            </span>
            Abrir en otra pestaña
          </a>
        </section>
      </div>
    </div>
  );
}
