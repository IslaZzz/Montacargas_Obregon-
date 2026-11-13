// Ícono de Material Symbols Rounded (se carga en index.html). Los nombres están en fonts.google.com/icons
export default function Icono({ nombre, relleno = false, className = "" }: { nombre: string; relleno?: boolean; className?: string }) {
  return (
    <span className={`icono material-symbols-rounded${relleno ? " icono-relleno" : ""} ${className}`} aria-hidden="true">
      {nombre}
    </span>
  );
}