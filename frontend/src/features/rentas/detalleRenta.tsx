import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { listarRentas } from "./rentasApi";
import type { Renta } from "./estadosRentaTypes";
import Chip from "../../components/chips";
import { Button } from "../../components/buttons";
import "../../styles/rentas/detalleRenta.css";

function formatearFecha(fecha?: string | null) {
  if (!fecha) return "No especificada";

  const valor = fecha.length === 10
    ? new Date(`${fecha}T12:00:00`)
    : new Date(fecha);

  if (Number.isNaN(valor.getTime())) return fecha;

  return new Intl.DateTimeFormat("es-MX", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(valor);
}

function formatearHora(fecha?: string | null) {
  if (!fecha) return "";

  const valor = new Date(fecha);
  if (Number.isNaN(valor.getTime())) return "";

  return new Intl.DateTimeFormat("es-MX", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  }).format(valor);
}

function obtenerNombreCliente(renta: Renta) {
  const cliente = renta.cliente;

  if (cliente.empresa) return cliente.empresa;

  return [
    cliente.nombre,
    cliente.apellidoPaterno,
    cliente.apellidoMaterno,
  ].filter(Boolean).join(" ");
}

export default function DetalleRenta() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [renta, setRenta] = useState<Renta | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function cargarRenta() {
      try {
        setLoading(true);

        const rentas = await listarRentas();
        const encontrada = rentas.find(
          item => String(item.id) === id
        );

        if (!encontrada) {
          setError("No se encontró la renta solicitada.");
          return;
        }

        setRenta(encontrada);
      } catch (err) {
        console.error("Error al cargar el detalle de la renta:", err);
        setError("No fue posible cargar la información de la renta.");
      } finally {
        setLoading(false);
      }
    }

    cargarRenta();
  }, [id]);

  if (loading) {
    return <main className="detalle-loading">Cargando renta...</main>;
  }

  if (error || !renta) {
    return (
      <main className="detalle-loading">
        <p>{error ?? "Renta no encontrada."}</p>
        <Button variant="secondary" onClick={() => navigate("/rentas")}>
          Volver a rentas
        </Button>
      </main>
    );
  }

  const cliente = renta.cliente as Renta["cliente"] & {
    correo?: string;
  };

  const nombreCliente = obtenerNombreCliente(renta);

  const estadoChip =
    renta.estado === "PROGRAMADA" ? "programado" :
    renta.estado === "EN_CURSO" ? "en-curso" :
    null;

  const cantidadEquipos = renta.equipos?.length ?? 0;

  const fechaEntrega = renta.entregaDesde
    ? formatearFecha(renta.entregaDesde)
    : formatearFecha(renta.fechaInicio);

  const horaEntrega = renta.entregaDesde && renta.entregaHasta
    ? `${formatearHora(renta.entregaDesde)} – ${formatearHora(renta.entregaHasta)}`
    : "Horario pendiente";

  const fechaFin = formatearFecha(renta.fechaFin);

  const duracionDias = Math.max(
    1,
    Math.ceil(
      (new Date(`${renta.fechaFin}T12:00:00`).getTime() -
        new Date(`${renta.fechaInicio}T12:00:00`).getTime()) /
        (1000 * 60 * 60 * 24)
    )
  );

  return (
    <main className="detalle-renta-page">

      {/* CONTENIDO CENTRAL */}
      <section className="detalle-renta-main">

        <button
          type="button"
          className="detalle-back"
          onClick={() => navigate("/rentas")}
        >
          <span className="material-symbols-rounded">arrow_back</span>
          Volver a rentas
        </button>

        <h1>Renta #{renta.folio}</h1>

        <div className="detalle-renta-heading">
          {estadoChip ? (
            <Chip category="renta" status={estadoChip} />
          ) : (
            <span className="detalle-status-neutral">
              {renta.estado}
            </span>
          )}

          <span className="detalle-letter-status">
            <span className="status-dot" />
            Carta: Sin generar
          </span>

          <span className="detalle-client-name">
            {nombreCliente}
          </span>
        </div>

        {/* AVISO DE CARTA RESPONSIVA */}
        <section className="carta-alerta">
          <div className="carta-alerta-title">
            <span className="material-symbols-rounded">
              description
            </span>
            <h2>Falta generar la carta responsiva</h2>
          </div>

          <p className="carta-alerta-description">
            Se crea en PDF con los datos del cliente, equipo y renta.
          </p>

          <p className="carta-alerta-note">
            Ya están cargados en la renta: no necesitas escribir nada.
          </p>

          <Button
            variant="primary"
            className="carta-generate-button"
            disabled
            title="La generación de PDF todavía no está conectada al backend"
          >
            <span className="material-symbols-rounded">
              note_add
            </span>
            Generar carta responsiva
          </Button>
        </section>

        {/* RESUMEN DE LA RENTA */}
        <section className="detalle-summary">

          <div className="detalle-summary-item">
            <span className="material-symbols-rounded">
              forklift
            </span>
            <div>
              <small>Equipos</small>
              <strong>
                {cantidadEquipos} {cantidadEquipos === 1 ? "equipo" : "equipos"}
              </strong>
            </div>
            <span className="material-symbols-rounded summary-arrow">
              chevron_right
            </span>
          </div>

          <div className="detalle-summary-item">
            <span className="material-symbols-rounded">
              description
            </span>
            <div>
              <small>Carta responsiva</small>
              <strong>Sin generar</strong>
            </div>
          </div>

          <div className="detalle-summary-item">
            <span className="material-symbols-rounded">
              local_shipping
            </span>
            <div>
              <small>Entrega</small>
              <strong>{fechaEntrega}</strong>
            </div>
          </div>

        </section>

        {/* EQUIPOS */}
        <section className="detalle-equipment-list">
          <h2>Equipos incluidos</h2>

          {renta.equipos?.map(equipo => (
            <div className="detalle-equipment" key={equipo.id}>
              <div>
                <strong>{equipo.numeroUnidad}</strong>
                <small>{equipo.numeroSerie}</small>
              </div>
              <span>{equipo.fase.replaceAll("_", " ")}</span>
            </div>
          ))}
        </section>

        <p className="detalle-permissions">
          <span className="material-symbols-rounded">lock</span>
          Gerencia y auxiliar generan y envían la carta. Auxiliar y
          traslados y operación registran la firma.
        </p>

      </section>

      {/* PANEL LATERAL */}
      <aside className="detalle-renta-sidebar">

        <h2>Datos de la renta</h2>

        <section className="detalle-info-block">
          <small>Contacto</small>
          <strong>{renta.contactoObra || nombreCliente}</strong>
          <span>{renta.telefonoContacto || "Teléfono no registrado"}</span>
        </section>

        <section className="detalle-info-block">
          <small>Correo electrónico</small>
          <strong>{cliente.correo || "Correo no registrado"}</strong>
        </section>

        <section className="detalle-info-block">
          <small>Domicilio de entrega</small>
          <strong>{renta.direccionObra || "Domicilio no registrado"}</strong>
        </section>

        <section className="detalle-info-block">
          <small>Periodo de renta</small>
          <strong>
            {formatearFecha(renta.fechaInicio)} – {fechaFin}
          </strong>
          <span>{duracionDias} días</span>
        </section>

        <section className="detalle-info-block">
          <small>Entrega</small>
          <strong>{fechaEntrega}</strong>
          <span>{horaEntrega}</span>
        </section>

        <section className="detalle-info-block">
          <small>Recolección</small>
          <strong>
            {renta.recoleccionDesde
              ? formatearFecha(renta.recoleccionDesde)
              : "Pendiente de programar"}
          </strong>
          <span>
            {renta.recoleccionDesde && renta.recoleccionHasta
              ? `${formatearHora(renta.recoleccionDesde)} – ${formatearHora(renta.recoleccionHasta)}`
              : ""}
          </span>
        </section>

      </aside>
    </main>
  );
}
