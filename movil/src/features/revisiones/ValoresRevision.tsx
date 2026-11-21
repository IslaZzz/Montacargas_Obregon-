import Chip from "../../components/Chip";
import { Dato, ListaDatos } from "../../components/Datos";
import { mostrarValor } from "../../utils/campos";
import type { ValorRevision } from "./revisionesTypes";
import { NOMBRE_ESTADO_INSPECCION, TONO_ESTADO_INSPECCION } from "./revisionesTypes";

/**
 * Lo que se anotó en una revisión, campo por campo y sin las fotos (esas van en GaleriaFotos).
 * Lo usan el resumen de la revisión y el mantenimiento que salió de ella.
 */
export default function ValoresRevision({ valores }: { valores: ValorRevision[] }) {
  const datos = valores.filter((v) => v.tipo !== "INSPECCION" && v.tipo !== "FOTO");
  const inspecciones = valores.filter((v) => v.tipo === "INSPECCION" && v.estado);

  return (
    <>
      {datos.length > 0 && (
        <ListaDatos>
          {datos.map((v) => (
            <Dato key={v.nombre} etiqueta={v.nombre}>
              {mostrarValor(v.valor, v.tipo, v.unidad)}
            </Dato>
          ))}
        </ListaDatos>
      )}
      {inspecciones.map((v) => (
        <div key={v.nombre} className="renglon separado">
          <span className="renglon-texto">
            <span className="renglon-titulo">{v.nombre}</span>
            {v.detalle && <span className="renglon-detalle">{v.detalle}</span>}
          </span>
          <Chip tono={TONO_ESTADO_INSPECCION[v.estado!]}>{NOMBRE_ESTADO_INSPECCION[v.estado!]}</Chip>
        </div>
      ))}
    </>
  );
}