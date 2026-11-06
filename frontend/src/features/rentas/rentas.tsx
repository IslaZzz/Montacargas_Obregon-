import { useEffect, useState } from "react";
import { listarRentas } from "./rentasApi";
import type { Renta } from "./estadosRentaTypes";

export default function Rentas() {
  const [rentas, setRentas] = useState<Renta[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const cargarRentas = async () => {
      try {
        setLoading(true);
        const data = await listarRentas();
        setRentas(data);
      } catch (err) {
        console.error("Error al cargar rentas:", err);
        setError("No fue posible cargar las rentas.");
      } finally {
        setLoading(false);
      }
    };

    cargarRentas();
  }, []);

  if (loading) return <p>Cargando rentas...</p>;
  if (error) return <p>{error}</p>;

  return (
    <main>
      <h1>Rentas</h1>
      <p>Rentas encontradas: {rentas.length}</p>

      <pre>
        {JSON.stringify(rentas, null, 2)}
      </pre>
    </main>
  );
}
