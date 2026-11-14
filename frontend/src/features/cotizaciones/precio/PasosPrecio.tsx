// Pasos de la pantalla del precio, compartidos por todas sus pantallas
export const PASOS_PRECIO = ["Precio de equipos", "Traslado", "Descuento", "Vigencia y condiciones"];

export default function PasosPrecio({ actual }: { actual: number }) {
  return (
    <ol className="nc-pasos">
      {PASOS_PRECIO.map((nombre, i) => (
        <li
          key={nombre}
          className={i === actual ? "activo" : i < actual ? "hecho" : undefined}
          aria-current={i === actual ? "step" : undefined}
        >
          <span className="nc-pasos-numero">{i < actual ? "✓" : i + 1}</span>
          {nombre}
        </li>
      ))}
    </ol>
  );
}