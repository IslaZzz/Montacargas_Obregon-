import type { ReactNode } from "react";
import { NOMBRE_TIPO } from "../sesion/sesionTypes";
import { useSesion } from "../sesion/useSesion";
import BarraSuperior from "./BarraSuperior";
import BotonCuenta from "./BotonCuenta";
import Encabezado from "./Encabezado";
import Icono from "./Icono";
import Pantalla from "./Pantalla";

/**
 * Pantalla de inicio de una sección (la que abre cada pestaña): marca arriba con el botón de cuenta,
 * encabezado con el nombre de quien usa la app y las pestañas abajo.
 */
export default function PantallaPrincipal({ titulo, children }: { titulo: string; children: ReactNode }) {
  const { personal } = useSesion();

  return (
    <Pantalla barraSuperior={<BarraSuperior accion={<BotonCuenta />} />} navegacion>
      <Encabezado titulo={titulo}>
        {personal && (
          <>
            <Icono nombre="badge" />
            <span>
              {personal.nombre} · {NOMBRE_TIPO[personal.tipo]}
            </span>
          </>
        )}
      </Encabezado>
      {children}
    </Pantalla>
  );
}