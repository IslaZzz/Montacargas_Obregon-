import EstadoVacio from "../../components/EstadoVacio";
import PantallaPrincipal from "../../components/PantallaPrincipal";

// Pantalla de inicio de la sección. La historia que la implemente reemplaza el contenido.
export default function MisTareas() {
  return (
    <PantallaPrincipal titulo="Mis tareas">
      <EstadoVacio>Aquí va la lista de traslados asignados.</EstadoVacio>
    </PantallaPrincipal>
  );
}