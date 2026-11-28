import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Modal from "../../components/Modal";
import { useTitulo } from "../../hooks/useTitulo";
import FormularioPersonal from "./FormularioPersonal";
import { actualizarPersonal, darDeBajaPersonal, obtenerPersonal } from "./personalApi";
import type { Personal } from "./personalTypes";
import { nombreCompleto } from "./personalTypes";

/** Editar personal (OHU-28): cambia los datos de alguien ya registrado o lo da de baja. */
export default function EditarPersonal() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [personal, setPersonal] = useState<Personal | null>(null);
  const [error, setError] = useState("");

  // Confirmación de la baja
  const [confirmandoBaja, setConfirmandoBaja] = useState(false);
  const [dandoDeBaja, setDandoDeBaja] = useState(false);
  const [errorBaja, setErrorBaja] = useState("");

  useTitulo(personal ? `Editar a ${nombreCompleto(personal)}` : "Editar personal");

  useEffect(() => {
    if (!id) return;
    let activo = true;
    obtenerPersonal(id)
      .then((p) => activo && setPersonal(p))
      .catch(() => activo && setError("No se encontró a esa persona."));
    return () => {
      activo = false;
    };
  }, [id]);

  if (!personal) {
    return (
      <div className="pagina">
        {error ? <Aviso tipo="error" titulo={error} /> : <p>Cargando…</p>}
      </div>
    );
  }

  const abrirBaja = () => {
    setErrorBaja("");
    setConfirmandoBaja(true);
  };

  const cerrarBaja = () => {
    if (!dandoDeBaja) setConfirmandoBaja(false);
  };

  const darDeBaja = async () => {
    setDandoDeBaja(true);
    setErrorBaja("");
    try {
      await darDeBajaPersonal(personal.id);
      navigate("/personal", { state: { aviso: `${nombreCompleto(personal)} quedó de baja.` } });
    } catch (err) {
      setErrorBaja(err instanceof Error ? err.message : "No fue posible dar de baja.");
      setDandoDeBaja(false);
    }
  };

  return (
    <>
      <FormularioPersonal
        // key: si cambia la persona, el formulario empieza con sus datos
        key={personal.id}
        titulo={`Editar a ${nombreCompleto(personal)}`}
        descripcion={
          personal.activo
            ? "Cambia los datos y presiona \"Guardar cambios\"."
            : "Esta persona está de baja: no se le asigna trabajo ni puede entrar a la app móvil."
        }
        textoGuardar="Guardar cambios"
        inicial={personal}
        avisoCodigo={`Con este código entra a la app móvil. Si lo cambias, tendrá que entrar con el nuevo (ahora es ${personal.codigo}).`}
        accionExtra={
          personal.activo ? (
            <Button type="button" variant="tertiary" onClick={abrirBaja}>
              <span className="material-symbols-rounded" aria-hidden="true">
                person_off
              </span>
              Dar de baja
            </Button>
          ) : undefined
        }
        onGuardar={async (datos) => {
          const actualizado = await actualizarPersonal(personal.id, datos);
          navigate("/personal", { state: { aviso: `Se guardaron los cambios de ${nombreCompleto(actualizado)}.` } });
        }}
      />

      {/* Confirmar la baja */}
      <Modal
        abierto={confirmandoBaja}
        titulo={`¿Dar de baja a ${nombreCompleto(personal)}?`}
        subtitulo={`Código ${personal.codigo}`}
        onCerrar={cerrarBaja}
        acciones={
          <>
            <Button type="button" variant="tertiary" onClick={cerrarBaja} disabled={dandoDeBaja}>
              Cancelar
            </Button>
            <Button type="button" variant="primary" onClick={darDeBaja} disabled={dandoDeBaja}>
              {dandoDeBaja ? "Guardando…" : "Dar de baja"}
            </Button>
          </>
        }
      >
        <p>
          Ya no aparecerá para asignarle tareas y, si tiene la app abierta, su sesión se cerrará. Sus registros
          anteriores se conservan.
        </p>
        {errorBaja && (
          <Aviso tipo="error" titulo="No se pudo dar de baja">
            {errorBaja}
          </Aviso>
        )}
      </Modal>
    </>
  );
}