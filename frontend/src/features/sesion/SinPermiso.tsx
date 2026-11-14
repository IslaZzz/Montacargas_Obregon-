import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "../../components/buttons";
import { useTitulo } from "../../hooks/useTitulo";
import { nombrePantalla, puesto, RUTA_AL_ENTRAR } from "./sesionUtils";
import { useSesion } from "./useSesion";
import "../../styles/sesion/sin-permiso.css";

// Lo que ve un auxiliar si abre por URL una pantalla de gerencia (HU-29)
export default function SinPermiso() {
  useTitulo("Sin permiso");
  const { sesion } = useSesion();
  const navegar = useNavigate();
  const pantalla = nombrePantalla(useLocation().pathname);

  if (!sesion) return null;

  return (
    <div className="pagina sin-permiso">
      <section className="sin-permiso-aviso" role="alert">
        <span className="sin-permiso-icono material-symbols-rounded" aria-hidden="true">
          lock
        </span>
        <span className="sin-permiso-rol">
          <span className="material-symbols-rounded" aria-hidden="true">
            person
          </span>
          Tu rol: {puesto(sesion.rol)}
        </span>
        <h1>No tienes permiso para ver esta pantalla</h1>
        <p>
          {pantalla ? `«${pantalla}» es` : "Esta pantalla es"} solo para gerencia. Si
          necesitas este acceso, pídeselo a gerencia.
        </p>
        <Button onClick={() => navegar(RUTA_AL_ENTRAR)}>
          <span className="material-symbols-rounded" aria-hidden="true">
            description
          </span>
          Ir a Cotizaciones
        </Button>
      </section>
    </div>
  );
}
