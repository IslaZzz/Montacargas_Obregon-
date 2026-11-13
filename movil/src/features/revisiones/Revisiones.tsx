import EstadoVacio from "../../components/EstadoVacio";
import PantallaPrincipal from "../../components/PantallaPrincipal";

// Pantalla de inicio de la sección. La historia que la implemente reemplaza el contenido.
export default function Revisiones() {
  return (
    <PantallaPrincipal titulo="Revisiones">
      <EstadoVacio>Aquí va la lista de revisiones asignadas.</EstadoVacio>
    </PantallaPrincipal>
  );
}