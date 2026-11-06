import { useCallback } from "react";
import { Link, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import Chip from "../../components/chips";
import { useTitulo } from "../../hooks/useTitulo";
import CartaResponsiva from "./CartaResponsiva";
import type { Carta, EstadoRenta } from "./cartasTypes";
import ChipCarta from "./ChipCarta";
import { useCarta } from "./useCarta";
import "../../styles/cartas/carta-responsiva.css";

const NOMBRE_RENTA: Record<EstadoRenta, string> = {
  PROGRAMADA: "Programada",
  EN_CURSO: "En curso",
  CERRADA: "Cerrada",
  CANCELADA: "Cancelada",
};

/**
 * Carta responsiva de una renta: /rentas/:id/carta.
 * PROVISIONAL mientras no existe el detalle de la renta (OHU-03): cuando exista,
 * el bloque <CartaResponsiva /> se pone ahí y esta pantalla se puede quitar.
 */
export default function CartaRenta() {
  const { id } = useParams();
  const { carta, setCarta, error } = useCarta(id);
  const actualizar = useCallback((nueva: Carta) => setCarta(nueva), [setCarta]);

  useTitulo(carta ? `Carta de la renta ${carta.folio}` : "Carta responsiva");

  return (
    <div className="pagina carta-pagina">
      <Link to="/apartados-proximos" className="carta-volver">
        <span className="material-symbols-rounded" aria-hidden="true">
          arrow_back
        </span>
        Volver a apartados próximos
      </Link>

      {error && <Aviso tipo="error" titulo={error} />}
      {!carta && !error && <p>Cargando carta…</p>}

      {carta && (
        <>
          <div className="carta-encabezado">
            <h1>Renta {carta.folio}.</h1>
            <div className="carta-chips">
              {carta.estadoRenta === "PROGRAMADA" ? (
                <Chip category="renta" status="programado">
                  Programada
                </Chip>
              ) : (
                <Chip category="renta" status="en-curso">
                  {NOMBRE_RENTA[carta.estadoRenta]}
                </Chip>
              )}
              <ChipCarta estado={carta.estado} />
            </div>
          </div>

          <CartaResponsiva carta={carta} onActualizada={actualizar} />

          <p className="carta-nota">
            <span className="material-symbols-rounded" aria-hidden="true">
              lock
            </span>
            Gerencia y auxiliar generan y envían la carta. Auxiliar y traslados registran la firma.
          </p>
        </>
      )}
    </div>
  );
}
