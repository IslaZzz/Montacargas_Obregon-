import { useNavigate } from "react-router-dom";
import { useTitulo } from "../../hooks/useTitulo";
import FormularioPersonal from "./FormularioPersonal";
import { registrarPersonal } from "./personalApi";
import { NOMBRE_TIPO, nombreCompleto } from "./personalTypes";

/** Registrar personal (OHU-26): un operador de traslado o un mecánico nuevo. */
export default function RegistrarPersonal() {
  useTitulo("Registrar personal");
  const navigate = useNavigate();

  return (
    <FormularioPersonal
      titulo="Registrar personal"
      descripcion="Registra a un operador de traslado o a un mecánico para poder asignarle trabajo."
      textoGuardar="Registrar personal"
      onGuardar={async (datos) => {
        const nuevo = await registrarPersonal(datos);
        navigate("/personal", {
          state: {
            aviso: `${NOMBRE_TIPO[nuevo.tipo]} registrado: ${nombreCompleto(nuevo)}, con el código ${nuevo.codigo}.`,
          },
        });
      }}
    />
  );
}
