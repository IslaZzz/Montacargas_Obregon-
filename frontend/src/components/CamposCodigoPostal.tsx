import { useId, useRef, useState } from "react";
import Campo from "./CampoForm";
import { buscarCodigoPostal } from "../services/codigosPostalesApi";

export interface DireccionDelCodigoPostal {
  colonia: string;
  ciudad: string;
  estado: string;
}

interface Props {
  codigoPostal: string;
  colonia: string;
  errorCodigoPostal?: string;
  errorColonia?: string;
  onCambiarCodigoPostal: (valor: string) => void;
  onCambiarColonia: (valor: string) => void;
  onEncontrado: (direccion: DireccionDelCodigoPostal) => void;
}

interface Busqueda {
  codigo: string;
  estado: "inactiva" | "buscando" | "encontrada" | "no-encontrada";
  colonias: string[];
}

const SIN_BUSQUEDA: Busqueda = { codigo: "", estado: "inactiva", colonias: [] };

const AYUDA: Record<Busqueda["estado"], string | undefined> = {
  inactiva: undefined,
  buscando: "Buscando el código postal…",
  encontrada: undefined,
  "no-encontrada": "No encontramos este código postal. Escribe los datos a mano.",
};

export default function CamposCodigoPostal({
  codigoPostal,
  colonia,
  errorCodigoPostal,
  errorColonia,
  onCambiarCodigoPostal,
  onCambiarColonia,
  onEncontrado,
}: Props) {
  const idColonias = useId();
  const [busqueda, setBusqueda] = useState<Busqueda>(SIN_BUSQUEDA);
  const ultimoBuscado = useRef("");

  const cambiarCodigoPostal = (valor: string) => {
    const codigo = valor.replace(/\D/g, "").slice(0, 5);
    onCambiarCodigoPostal(codigo);
    if (codigo === codigoPostal) return;
    ultimoBuscado.current = codigo;

    if (codigo.length < 5) {
      setBusqueda(SIN_BUSQUEDA);
      return;
    }

    setBusqueda({ codigo, estado: "buscando", colonias: [] });
    buscarCodigoPostal(codigo)
      .then((datos) => {
        if (ultimoBuscado.current !== codigo) return;
        setBusqueda({ codigo, estado: "encontrada", colonias: datos.colonias });
        onEncontrado({
          colonia: datos.colonias.length === 1 ? datos.colonias[0] : "",
          ciudad: datos.ciudad,
          estado: datos.estado,
        });
      })
      .catch(() => {
        if (ultimoBuscado.current === codigo)
          setBusqueda({ codigo, estado: "no-encontrada", colonias: [] });
      });
  };

  const vigente = busqueda.codigo === codigoPostal ? busqueda : SIN_BUSQUEDA;
  const variasColonias = vigente.colonias.length > 1;

  return (
    <>
      <Campo
        etiqueta="Código postal"
        inputMode="numeric"
        maxLength={5}
        value={codigoPostal}
        error={errorCodigoPostal}
        ayuda={AYUDA[vigente.estado]}
        onChange={cambiarCodigoPostal}
      />
      <Campo
        etiqueta="Colonia"
        maxLength={100}
        list={variasColonias ? idColonias : undefined}
        placeholder={variasColonias ? "Elige o escribe la colonia" : undefined}
        value={colonia}
        error={errorColonia}
        ayuda={
          variasColonias
            ? `Este código postal tiene ${vigente.colonias.length} colonias.`
            : undefined
        }
        onChange={onCambiarColonia}
      />
      {variasColonias && (
        <datalist id={idColonias}>
          {vigente.colonias.map((nombre) => (
            <option key={nombre} value={nombre} />
          ))}
        </datalist>
      )}
    </>
  );
}
