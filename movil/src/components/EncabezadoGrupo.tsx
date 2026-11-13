/** Título de un grupo de la lista con un dato a la derecha: "Hoy · 8 oct" … "1 pendiente" */
export default function EncabezadoGrupo({ titulo, detalle }: { titulo: string; detalle?: string }) {
  return (
    <div className="encabezado-grupo">
      <h3>{titulo}</h3>
      {detalle && <span>{detalle}</span>}
    </div>
  );
}