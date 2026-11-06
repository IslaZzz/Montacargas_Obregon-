import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { Button } from "../../components/buttons";
import { obtenerCliente } from "./clientesApi";
import type { Cliente } from "./clientesTypes";
import { useTitulo } from "../../hooks/useTitulo";

// 6871234567 pasa a 687 123 4567
const formatearTelefono = (telefono: string) =>
  telefono.replace(/(\d{3})(\d{3})(\d{4})/, "$1 $2 $3");

// 2026-10-08 pasa a 08/10/2026
const formatearFecha = (fecha: string) => {
  const [anio, mes, dia] = fecha.split("-");
  return `${dia}/${mes}/${anio}`;
};

export default function DetalleCliente() {
  useTitulo("Ver cliente");
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [cliente, setCliente] = useState<Cliente | null>(null);
  const [error, setError] = useState("");

  // Llega en true cuando se viene de guardar un cliente
  const recienGuardado = Boolean(location.state?.recienGuardado);

  // Carga el cliente al abrir la pantalla
  useEffect(() => {
    let activo = true;
    obtenerCliente(Number(id))
      .then((datos) => {
        if (activo) setCliente(datos);
      })
      .catch(() => {
        if (activo) setError("No fue posible cargar el cliente.");
      });
    return () => {
      activo = false;
    };
  }, [id]);

  // Error al cargar
  if (error) {
    return (
      <div className="pagina">
        <div className="aviso-error" role="alert">
          <span className="material-symbols-rounded">error</span>
          <div>
            <strong>{error}</strong>
            <p>Regresa al listado e intenta de nuevo.</p>
          </div>
        </div>
        <div>
          <Button variant="secondary" onClick={() => navigate("/clientes")}>
            Volver a clientes
          </Button>
        </div>
      </div>
    );
  }

  // Mientras carga
  if (!cliente) {
    return (
      <div className="pagina">
        <p>Cargando cliente...</p>
      </div>
    );
  }

  const nombreCompleto = [
    cliente.nombre,
    cliente.apellidoPaterno,
    cliente.apellidoMaterno,
  ]
    .filter(Boolean)
    .join(" ");

  // Empresa y fecha de registro, solo si existen
  const subtitulo = [
    cliente.empresa,
    cliente.fechaRegistro && `Registrado el ${formatearFecha(cliente.fechaRegistro)}`,
  ]
    .filter(Boolean)
    .join(" · ");

  const datos = [
    { etiqueta: "Nombre del contacto", valor: nombreCompleto },
    { etiqueta: "Empresa", valor: cliente.empresa || "—" },
    { etiqueta: "Teléfono", valor: formatearTelefono(cliente.telefono) },
    { etiqueta: "Correo electrónico", valor: cliente.correo },
  ];

  return (
    <div className="pagina">
      {/* Aviso de éxito, solo al venir de guardar */}
      {recienGuardado && (
        <div className="aviso-exito" role="status">
          <span className="material-symbols-rounded">check_circle</span>
          <div>
            <strong>Cliente guardado</strong>
            <p>Ya puedes elegirlo al crear una cotización.</p>
          </div>
        </div>
      )}

      {/* Título y acciones */}
      <div className="titulo-acciones">
        <div className="titulo-acciones-texto">
          <h1>{nombreCompleto}</h1>
          {subtitulo && <p>{subtitulo}</p>}
        </div>
        <div className="titulo-acciones-botones">
          {/* Pendiente: pantalla de edición */}
          <Button type="button" variant="secondary">
            <span className="material-symbols-rounded">edit</span>
            Editar datos
          </Button>
          {/* Pendiente: pantalla de nueva cotización */}
          <Button
            type="button"
            variant="primary"
            onClick={() =>
              navigate("/cotizaciones/nueva", {
                state: { clienteId: cliente.id },
              })
            }
          >
            Crear cotización para este cliente →
          </Button>
        </div>
      </div>

      <div className="secciones">
        {/* Tarjeta de contacto */}
        <section className="tarjeta">
          <h2>
            <span className="paso">1</span> Datos de contacto
          </h2>
          <div className="datos">
            {datos.map((d) => (
              <div className="dato" key={d.etiqueta}>
                <span className="dato-etiqueta">{d.etiqueta}</span>
                <span className="dato-valor">{d.valor}</span>
              </div>
            ))}
          </div>
        </section>

        {/* Tarjeta de razones sociales */}
        <section className="tarjeta">
          <h2>
            <span className="paso">2</span> Razones sociales para facturar (
            {cliente.razonesSociales.length})
          </h2>

          {cliente.razonesSociales.map((r) => (
            <div className="razon-detalle" key={r.id}>
              <strong>{r.nombre}</strong>
              <span className="razon-detalle-rfc">RFC: {r.rfc}</span>
              <span className="razon-detalle-direccion">
                {r.direccionFiscal.calle} {r.direccionFiscal.numero}, Col.{" "}
                {r.direccionFiscal.colonia}, {r.direccionFiscal.ciudad}, C.P.{" "}
                {r.direccionFiscal.codigoPostal}
              </span>
            </div>
          ))}
        </section>
      </div>
    </div>
  );
}