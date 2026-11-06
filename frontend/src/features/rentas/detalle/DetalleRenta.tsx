import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Avatar from "../../../components/Avatar";
import Aviso from "../../../components/Aviso";
import Chip from "../../../components/chips";
import { useTitulo } from "../../../hooks/useTitulo";
import { formatoHora } from "../../../utils/fechas";
import { formatoTelefono } from "../../../utils/formato";
import CartaResponsiva from "../../cartas/CartaResponsiva";
import type { Carta } from "../../cartas/cartasTypes";
import { NOMBRE_ESTADO as NOMBRE_CARTA } from "../../cartas/cartasUtils";
import { useCarta } from "../../cartas/useCarta";
import { obtenerDetalleRenta } from "./detalleApi";
import type { DetalleRenta as Detalle, EstadoRenta } from "./detalleTypes";
import { cuandoEs, diaLargo, diasDeRenta, periodo } from "./detalleUtils";
import RecoleccionRenta from "./RecoleccionRenta";
import "../../../styles/clientes/registrar-cliente.css";
import "../../../styles/cartas/carta-responsiva.css";
import "../../../styles/rentas/detalle-renta.css";

function ChipRenta({ estado }: { estado: EstadoRenta }) {
  switch (estado) {
    case "PROGRAMADA":
      return <Chip category="renta" status="programado">Programada</Chip>;
    case "EN_CURSO":
      return <Chip category="renta" status="en-curso">En curso</Chip>;
    case "CERRADA":
      return <Chip category="traslado" status="ok">Cerrada</Chip>;
    case "CANCELADA":
      return <Chip category="mantenimiento" status="cancelado">Cancelada</Chip>;
  }
}

/**
 * Detalle de una renta (OHU-03): /rentas/:id.
 * Programada: arriba va la carta (OHU-04 y OHU-05). En curso: arriba va la recolección (OHU-12),
 * y si la carta está generada sin firmar, su bloque sigue abajo para registrar la firma.
 */
export default function DetalleRenta() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { carta, setCarta, error: errorCarta } = useCarta(id);

  const [detalle, setDetalle] = useState<Detalle | null>(null);
  const [error, setError] = useState("");
  const [exito, setExito] = useState<{ titulo: string; texto: string } | null>(null);

  useTitulo(detalle ? `Renta ${detalle.folio}` : "Renta");

  useEffect(() => {
    if (!id) return;
    let activo = true;
    obtenerDetalleRenta(id)
      .then((d) => activo && setDetalle(d))
      .catch(() => activo && setError("No fue posible cargar la renta."));
    return () => {
      activo = false;
    };
  }, [id]);

  function recoleccionActualizada(nuevo: Detalle, titulo: string, texto: string) {
    setDetalle(nuevo);
    setExito({ titulo, texto });
  }

  // Al firmarse, el bloque cambia al de recolección; el aviso confirma que sí se guardó
  function cartaActualizada(nueva: Carta) {
    setCarta(nueva);
    if (nueva.estado === "FIRMADA") {
      setExito({
        titulo: "Firma registrada",
        texto:
          detalle?.estado === "PROGRAMADA"
            ? "La carta quedó firmada. La recolección se coordina cuando la renta esté en curso."
            : "La carta quedó firmada.",
      });
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  }
  if (!detalle) {
    return (
      <div className="pagina">
        {error ? <Aviso tipo="error" titulo={error} /> : <p>Cargando renta…</p>}
      </div>
    );
  }

  const programada = detalle.estado === "PROGRAMADA";
  const operador = detalle.trasladoRecoleccion?.responsable ?? null;
  const firmada = carta?.estado === "FIRMADA";

  // Programada: la carta mientras no se firme. En curso o después: la recolección
  let bloque;
  if (programada && carta === null && !errorCarta) {
    bloque = <p>Cargando carta…</p>;
  } else if (programada && carta !== null && !firmada) {
    bloque = <CartaResponsiva carta={carta} onActualizada={cartaActualizada} />;
  } else {
    bloque = <RecoleccionRenta detalle={detalle} onActualizada={recoleccionActualizada} />;
  }

  // En curso con la carta generada sin firmar: la firma todavía se puede registrar
  const cartaPorFirmar = detalle.estado === "EN_CURSO" && carta !== null && carta.puedeFirmar && !firmada;

  return (
    <div className="detalle-renta">
      <div className="pagina">
        <button type="button" className="detalle-renta-volver" onClick={() => navigate(-1)}>
          <span className="material-symbols-rounded" aria-hidden="true">arrow_back</span>
          Volver
        </button>

        <div className="detalle-renta-encabezado">
          <h1>Renta {detalle.folio}.</h1>
          <div className="detalle-renta-cliente">
            <ChipRenta estado={detalle.estado} />
            <span>{detalle.cliente}</span>
          </div>
        </div>

        {exito && (
          <Aviso tipo="exito" titulo={exito.titulo} onCerrar={() => setExito(null)}>
            {exito.texto}
          </Aviso>
        )}
        {errorCarta && <Aviso tipo="error" titulo={errorCarta} />}

        <div className="detalle-renta-contenido">
          {bloque}

          {cartaPorFirmar && <CartaResponsiva carta={carta} onActualizada={cartaActualizada} />}

          {/* Equipos, carta y fecha clave: solo informativos */}
          <div className="detalle-renta-franja">
            <div className="detalle-renta-dato">
              <span className="material-symbols-rounded detalle-renta-icono" aria-hidden="true">forklift</span>
              <span className="detalle-renta-dato-texto">
                <small>Equipos</small>
                <strong>
                  {detalle.equipos.length} {detalle.equipos.length === 1 ? "equipo" : "equipos"}
                </strong>
              </span>
            </div>

            <div className="detalle-renta-dato">
              <span
                className={"material-symbols-rounded detalle-renta-icono" + (firmada ? " detalle-renta-icono-ok" : "")}
                aria-hidden="true"
              >
                {firmada ? "check" : "description"}
              </span>
              <span className="detalle-renta-dato-texto">
                <small>Carta responsiva</small>
                <strong className={firmada ? "detalle-renta-ok" : undefined}>
                  {carta ? NOMBRE_CARTA[carta.estado] : "–"}
                </strong>
              </span>
            </div>

            <div className="detalle-renta-dato">
              <span className="material-symbols-rounded detalle-renta-icono" aria-hidden="true">
                {programada ? "local_shipping" : "event"}
              </span>
              <span className="detalle-renta-dato-texto">
                <small>{programada ? "Entrega" : "Fin de renta"}</small>
                <strong>
                  {programada
                    ? detalle.entregaDesde
                      ? cuandoEs(detalle.entregaDesde)
                      : "Sin horario"
                    : cuandoEs(detalle.fechaFin)}
                </strong>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Datos de la renta */}
      <aside className="detalle-renta-panel" aria-labelledby="titulo-datos-renta">
        <div className="detalle-renta-panel-encabezado">
          <h2 id="titulo-datos-renta">Datos de la renta</h2>
        </div>
        <div className="detalle-renta-panel-dato">
          <small>Contacto</small>
          <strong>{detalle.contacto}</strong>
          <span>{formatoTelefono(detalle.telefono)}</span>
        </div>
        <div className="detalle-renta-panel-dato">
          <small>{programada ? "Domicilio" : "Domicilio de recolección"}</small>
          <strong>{detalle.direccion}</strong>
        </div>
        <div className="detalle-renta-panel-dato">
          <small>Periodo de renta</small>
          <strong>{periodo(detalle.fechaInicio, detalle.fechaFin)}</strong>
          <span>{diasDeRenta(detalle.fechaInicio, detalle.fechaFin)} días</span>
        </div>
        {programada ? (
          <div className="detalle-renta-panel-dato">
            <small>Entrega</small>
            {detalle.entregaDesde && detalle.entregaHasta ? (
              <>
                <strong>{diaLargo(detalle.entregaDesde)}</strong>
                <span>
                  {formatoHora(detalle.entregaDesde)} – {formatoHora(detalle.entregaHasta)}
                </span>
              </>
            ) : (
              <strong>Sin horario</strong>
            )}
          </div>
        ) : (
          <div className="detalle-renta-panel-dato">
            <small>Operador de recolección</small>
            {operador ? (
              <div className="recoleccion-operador">
                <Avatar nombre={operador} />
                <strong>{operador}</strong>
              </div>
            ) : (
              <strong>Sin asignar</strong>
            )}
          </div>
        )}
      </aside>
    </div>
  );
}