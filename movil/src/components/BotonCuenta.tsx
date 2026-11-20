import { useNavigate } from "react-router-dom";
import { hora } from "../utils/fechas";
import { useSesion } from "../sesion/useSesion";
import Icono from "./Icono";

/** Botón de la barra superior para cerrar la sesión; al pasar el dedo o el cursor dice hasta qué hora dura. */
export default function BotonCuenta() {
  const navigate = useNavigate();
  const { venceEn, salir } = useSesion();

  async function cerrar() {
    await salir();
    navigate("/entrar", { replace: true });
  }

  const detalle = venceEn ? ` (tu sesión vence a las ${hora(venceEn)})` : "";
  return (
    <button type="button" className="boton-icono" aria-label={`Cerrar sesión${detalle}`} title={`Cerrar sesión${detalle}`} onClick={cerrar}>
      <Icono nombre="logout" />
    </button>
  );
}
