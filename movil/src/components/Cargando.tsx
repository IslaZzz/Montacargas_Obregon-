/** Mientras llegan los datos. */
export default function Cargando({ texto = "Cargando…", pantallaCompleta = false }: { texto?: string; pantallaCompleta?: boolean }) {
  return (
    <div className={`cargando${pantallaCompleta ? " cargando-pantalla" : ""}`} role="status">
      <span className="cargando-circulo" aria-hidden="true" />
      {texto}
    </div>
  );
}