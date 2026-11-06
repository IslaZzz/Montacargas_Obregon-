import "../styles/avatar.css";

// Círculo con las iniciales de una persona: "Rafael Amaya" → "RA"
export default function Avatar({ nombre }: { nombre: string }) {
  const iniciales = nombre
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("");

  return (
    <span className="avatar" aria-hidden="true">
      {iniciales}
    </span>
  );
}