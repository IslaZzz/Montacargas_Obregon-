import EstadoVacio from "../../components/EstadoVacio";
import PantallaPrincipal from "../../components/PantallaPrincipal";

// Pantalla de inicio de la sección. La historia que la implemente reemplaza el contenido.
export default function Historial() {
  return (
    <PantallaPrincipal titulo="Historial">
      <EstadoVacio>Aquí va el historial de revisiones y mantenimientos terminados.</EstadoVacio>
    </PantallaPrincipal>
  );
}