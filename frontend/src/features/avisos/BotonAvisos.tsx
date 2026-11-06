import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import type { ApartadoProximo } from "./avisosTypes";
import { accionCarta, claveAviso, nombreCarta, rutaCarta, textoInicio, unidades } from "./avisosUtils";
import { useApartadosProximos } from "./useApartadosProximos";
import "../../styles/apartados/panel-avisos.css";

interface ItemAvisoProps {
  aviso: ApartadoProximo;
  nuevo: boolean;
  onIr: () => void; // Cierra la lista al ir a la carta
}

function ItemAviso({ aviso, nuevo, onIr }: ItemAvisoProps) {
  const accion = accionCarta(aviso);
  const inicio = textoInicio(aviso).replace(",", ""); // "Inicia mañana jue 8 oct"
  return (
    <li className={`panel-aviso${nuevo ? " panel-aviso-nuevo" : ""}`}>
      <span className="panel-aviso-icono material-symbols-rounded" aria-hidden="true">
        event_upcoming
      </span>
      <div className="panel-aviso-cuerpo">
        <p className="panel-aviso-titulo">
          Apartado próximo: Renta {aviso.folio}
          {nuevo && <span className="avisos-oculto"> (nuevo)</span>}
        </p>
        <p>
          {aviso.cliente}
          {aviso.equipos.length > 0 && `, ${unidades(aviso)}`}
        </p>
        <p>
          {inicio}, carta {nombreCarta(aviso).toLowerCase()}
        </p>
        <Link to={rutaCarta(aviso)} className="panel-aviso-accion" onClick={onIr}>
          {accion.texto}
          <span className="material-symbols-rounded" aria-hidden="true">
            arrow_forward
          </span>
        </Link>
      </div>
    </li>
  );
}

// Botón "Avisos" del encabezado con la lista desplegable (OHU-24)
export default function BotonAvisos() {
  const { apartados, error, noLeidos, marcarComoLeidos } = useApartadosProximos();
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef<HTMLDivElement>(null);

  // Se cierra al tocar fuera o con Escape
  useEffect(() => {
    if (!abierto) return;
    const alTocar = (e: MouseEvent) => {
      if (!contenedor.current?.contains(e.target as Node)) setAbierto(false);
    };
    const alTeclear = (e: KeyboardEvent) => {
      if (e.key === "Escape") setAbierto(false);
    };
    document.addEventListener("mousedown", alTocar);
    document.addEventListener("keydown", alTeclear);
    return () => {
      document.removeEventListener("mousedown", alTocar);
      document.removeEventListener("keydown", alTeclear);
    };
  }, [abierto]);

  const nuevos = new Set(noLeidos.map(claveAviso));
  const cantidad = noLeidos.length;

  return (
    <div className="avisos-contenedor" ref={contenedor}>
      <button
        type="button"
        className="encabezado-btn"
        aria-expanded={abierto}
        aria-controls="panel-avisos"
        aria-label={cantidad > 0 ? `Avisos, ${cantidad} sin leer` : "Avisos"}
        onClick={() => setAbierto(!abierto)}
      >
        <span className="avisos-campana">
          <span className="material-symbols-rounded" aria-hidden="true">
            {cantidad > 0 ? "notification_important" : "notifications"}
          </span>
          {cantidad > 0 && (
            <span className="avisos-contador" aria-hidden="true">
              {cantidad > 9 ? "9+" : cantidad}
            </span>
          )}
        </span>
        <span className="encabezado-texto">Avisos</span>
      </button>

      {abierto && (
        <section id="panel-avisos" className="panel-avisos" aria-label="Avisos">
          <header className="panel-avisos-encabezado">
            <h2>Avisos</h2>
            <button
              type="button"
              className="panel-avisos-marcar"
              onClick={marcarComoLeidos}
              disabled={cantidad === 0}
            >
              Marcar como leídos
            </button>
          </header>

          {error && apartados === null ? (
            <p className="panel-avisos-mensaje">No se pudieron cargar los avisos.</p>
          ) : apartados === null ? (
            <p className="panel-avisos-mensaje">Cargando avisos…</p>
          ) : apartados.length === 0 ? (
            <p className="panel-avisos-mensaje">
              <span className="material-symbols-rounded" aria-hidden="true">
                check_circle
              </span>
              Todo listo: no hay avisos pendientes.
            </p>
          ) : (
            <ul className="panel-avisos-lista">
              {apartados.map((a) => (
                <ItemAviso
                  key={a.rentaId}
                  aviso={a}
                  nuevo={nuevos.has(claveAviso(a))}
                  onIr={() => setAbierto(false)}
                />
              ))}
            </ul>
          )}

          <Link
            to="/apartados-proximos"
            className="panel-avisos-todos"
            onClick={() => setAbierto(false)}
          >
            Ver todos los apartados próximos
            <span className="material-symbols-rounded" aria-hidden="true">
              arrow_forward
            </span>
          </Link>
        </section>
      )}
    </div>
  );
}
