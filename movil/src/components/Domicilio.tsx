import Icono from "./Icono";
import { lineasDomicilio, urlMapa } from "../utils/domicilio";
import type { Domicilio as DatosDomicilio } from "../utils/domicilio";

/**
 * Muestra un domicilio. No importa si llegó en un solo texto o por partes (código postal):
 * las líneas las arma utils/domicilio.ts.
 */
export default function Domicilio({ domicilio, mapa = false }: { domicilio: DatosDomicilio; mapa?: boolean }) {
  return (
    <div className="domicilio">
      <address>
        {lineasDomicilio(domicilio).map((linea) => (
          <span key={linea}>{linea}</span>
        ))}
      </address>
      {mapa && (
        <a className="domicilio-mapa" href={urlMapa(domicilio)} target="_blank" rel="noreferrer">
          <Icono nombre="map" />
          Abrir en mapas
        </a>
      )}
    </div>
  );
}