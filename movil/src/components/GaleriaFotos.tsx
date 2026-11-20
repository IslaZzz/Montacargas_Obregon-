import { useEffect, useState } from "react";
import { listarFotos } from "../services/fotosApi";
import type { Foto, OrigenFoto } from "../services/fotosApi";

/** Las fotos que ya se subieron de algo, para consultarlas (resúmenes del historial). */
export default function GaleriaFotos({ origen, referenciaId }: { origen: OrigenFoto; referenciaId: number }) {
  const [fotos, setFotos] = useState<Foto[] | null>(null);

  useEffect(() => {
    listarFotos(origen, referenciaId)
      .then(setFotos)
      .catch(() => setFotos([]));
  }, [origen, referenciaId]);

  if (!fotos) return null;
  if (fotos.length === 0) return <p className="estado-vacio">Sin fotos.</p>;

  return (
    <div className="galeria">
      {fotos.map((foto) => (
        <a key={foto.id} className="galeria-foto" href={foto.url} target="_blank" rel="noreferrer">
          <img src={foto.url} alt={foto.etiqueta} loading="lazy" />
          <span>{foto.etiqueta}</span>
        </a>
      ))}
    </div>
  );
}