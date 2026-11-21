import { useId } from "react";
import { Link } from "react-router-dom";
import type { Cotizacion } from "./cotizacionesTypes";
import { motivoSinPdf } from "./cotizacionesUtils";

interface BotonVerPdfProps {
  cotizacion: Cotizacion;
  primario?: boolean; // Relleno en Borrador; con borde en los demás estados
  className?: string;
}

// Botón "Ver PDF" del detalle. Sin equipos o en autorización no hay PDF: queda deshabilitado y lo explica.
export default function BotonVerPdf({
  cotizacion,
  primario = false,
  className = "",
}: BotonVerPdfProps) {
  const idAyuda = useId();
  const clases = `btn ${primario ? "btn-primary" : "btn-tertiary"} btn-grande`;
  const contenido = (
    <>
      <span className="material-symbols-rounded" aria-hidden="true">
        description
      </span>
      Ver PDF
    </>
  );

  const motivo = motivoSinPdf(cotizacion);

  if (motivo) {
    return (
      <div className={`ver-pdf-deshabilitado ${className}`}>
        <button type="button" className={clases} disabled aria-describedby={idAyuda}>
          {contenido}
        </button>
        <span id={idAyuda}>{motivo}</span>
      </div>
    );
  }

  return (
    <Link
      to={`/cotizaciones/${cotizacion.id}/pdf`}
      className={`${clases} ${className}`}
    >
      {contenido}
    </Link>
  );
}
