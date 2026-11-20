import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { useTitulo } from "../../hooks/useTitulo";
import FormularioPersonal from "./FormularioPersonal";
import { actualizarPersonal, obtenerPersonal } from "./personalApi";
import type { Personal } from "./personalTypes";
import { nombreCompleto } from "./personalTypes";

/** Editar personal (OHU-28): cambia los datos de alguien ya registrado. */
export default function EditarPersonal() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [personal, setPersonal] = useState<Personal | null>(null);
  const [error, setError] = useState("");

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

  return (
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
      onGuardar={async (datos) => {
        const actualizado = await actualizarPersonal(personal.id, datos);
        navigate("/personal", { state: { aviso: `Se guardaron los cambios de ${nombreCompleto(actualizado)}.` } });
      }}
    />
  );
}
