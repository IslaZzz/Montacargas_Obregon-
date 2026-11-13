import { useNavigate } from "react-router-dom";
import Icono from "./Icono";

/** Botón de la barra superior para cambiar de persona (mientras no hay inicio de sesión). */
export default function BotonCuenta() {
  const navigate = useNavigate();
  return (
    <button type="button" className="boton-icono" aria-label="Cambiar de usuario" onClick={() => navigate("/entrar")}>
      <Icono nombre="account_circle" />
    </button>
  );
}