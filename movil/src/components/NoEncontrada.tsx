import { useNavigate } from "react-router-dom";
import Resultado from "./Resultado";

export default function NoEncontrada() {
  const navigate = useNavigate();
  return (
    <Resultado
      tipo="error"
      icono="search_off"
      titulo="Pantalla no encontrada"
      texto="La dirección no existe o cambió."
      accion={{ texto: "Ir al inicio", icono: "home", onClick: () => navigate("/", { replace: true }) }}
    />
  );
}