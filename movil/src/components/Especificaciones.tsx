import { mostrarValor } from "../utils/campos";
import { Dato } from "./Datos";

// Equivale a EspecificacionResponse: un dato del modelo que pide su categoría
export interface Especificacion {
  campoId: number;
  nombre: string;
  tipo: "TEXTO" | "ENTERO" | "DECIMAL" | "BOOLEANO" | "FECHA" | "LISTA";
  unidad: string | null;
  orden: number;
  valor: string;
}

/** Las especificaciones del modelo como renglones de una ListaDatos. Solo lectura. */
export default function Especificaciones({ especificaciones }: { especificaciones: Especificacion[] }) {
  return (
    <>
      {especificaciones.map((e) => (
        <Dato key={e.campoId} etiqueta={e.nombre}>
          {mostrarValor(e.valor, e.tipo, e.unidad)}
        </Dato>
      ))}
    </>
  );
}