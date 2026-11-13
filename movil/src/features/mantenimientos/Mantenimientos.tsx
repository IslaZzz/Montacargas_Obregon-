import EstadoVacio from "../../components/EstadoVacio";
import PantallaPrincipal from "../../components/PantallaPrincipal";

// Pantalla de inicio de la sección. La historia que la implemente reemplaza el contenido.
export default function Mantenimientos() {
  return (
    <PantallaPrincipal titulo="Mantenimientos">
      <EstadoVacio>Aquí va la lista de mantenimientos asignados.</EstadoVacio>
    </PantallaPrincipal>
  );
}