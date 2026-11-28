import { useState } from "react";
import Aviso from "../../components/Aviso";
import Boton from "../../components/Boton";
import Chip from "../../components/Chip";
import { Dato, ListaDatos } from "../../components/Datos";
import CampoFotos from "../../components/formulario/CampoFotos";
import CampoTexto from "../../components/formulario/CampoTexto";
import { Tarjeta } from "../../components/Tarjeta";
import { mensajeDeError } from "../../services/api";
import type { Foto } from "../../services/fotosApi";
import { adjuntarCarta, descargarCarta } from "./trasladosApi";
import type { CartaTarea, DetalleTarea } from "./trasladosTypes";

const FOTO_CARTA = "Carta firmada";

interface Props {
  tarea: DetalleTarea;
  onAdjuntada: (tarea: DetalleTarea) => void;
}

/**
 * OHU11: la carta responsiva de la entrega. Sin la carta firmada no se registra la entrega;
 * si el cliente la firma al recibir, el operador toma la foto y la adjunta aquí.
 */
export default function CartaResponsivaTarea({ tarea, onAdjuntada }: Props) {
  const carta = tarea.carta as CartaTarea;
  const [adjuntando, setAdjuntando] = useState(false);
  const [firmante, setFirmante] = useState("");
  const [foto, setFoto] = useState<{ archivo: File; vista: Foto } | null>(null);
  const [errores, setErrores] = useState<{ firmante?: string; foto?: string }>({});
  const [fallo, setFallo] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function registrar() {
    const faltan = {
      firmante: firmante.trim() ? undefined : "Escribe el nombre de quien firma.",
      foto: foto ? undefined : "Toma la foto de la carta firmada.",
    };
    setErrores(faltan);
    if (faltan.firmante || faltan.foto || !foto) return;

    setEnviando(true);
    setFallo(null);
    try {
      onAdjuntada(await adjuntarCarta(tarea.id, firmante.trim(), foto.archivo));
      setAdjuntando(false);
    } catch (e) {
      setFallo(mensajeDeError(e, "No se pudo adjuntar la carta."));
    } finally {
      setEnviando(false);
    }
  }

  async function ver() {
    // La ventana se abre antes de descargar; si se abre después, el navegador la bloquea
    const ventana = window.open("", "_blank");
    try {
      const url = URL.createObjectURL(await descargarCarta(tarea.id));
      if (ventana) ventana.location.href = url;
      else window.location.href = url;
    } catch (e) {
      ventana?.close();
      setFallo(mensajeDeError(e, "No se pudo abrir la carta."));
    }
  }

  return (
    <Tarjeta borde={carta === "FIRMADA" ? "normal" : "atencion"}>
      <h3>Carta responsiva</h3>
      <ListaDatos>
        <Dato etiqueta="Estado">
          <Chip tono={carta === "FIRMADA" ? "verde" : "amarillo"}>{carta === "FIRMADA" ? "Firmada" : "Pendiente"}</Chip>
        </Dato>
      </ListaDatos>

      {carta === "FIRMADA" && (
        <>
          <p className="tarjeta-linea">Carta firmada adjunta</p>
          <Boton variante="secundario" icono="description" onClick={ver}>
            Ver carta
          </Boton>
        </>
      )}

      {carta !== "FIRMADA" && (
        <Aviso tipo="error" titulo="Obligatoria: sin la carta firmada no se puede registrar la entrega">
          {carta === "SIN_GENERAR" ? "La oficina todavía no genera la carta de esta renta." : undefined}
        </Aviso>
      )}

      {carta === "PENDIENTE" && !adjuntando && (
        <Boton variante="secundario" icono="attach_file" onClick={() => setAdjuntando(true)}>
          Adjuntar carta firmada
        </Boton>
      )}

      {carta === "PENDIENTE" && adjuntando && (
        <>
          <CampoTexto
            etiqueta="Nombre de quien firma"
            valor={firmante}
            onCambio={setFirmante}
            obligatorio
            maxLength={150}
            error={errores.firmante}
          />
          <CampoFotos
            etiqueta="Foto de la carta firmada"
            requeridas={[FOTO_CARTA]}
            subidas={{ [FOTO_CARTA]: foto?.vista }}
            onSubir={async (_, archivo) => {
              setFoto({
                archivo,
                vista: { id: 0, etiqueta: FOTO_CARTA, url: URL.createObjectURL(archivo), subidaEn: "" },
              });
            }}
            obligatorio
            error={errores.foto}
          />
          <Boton icono="check" cargando={enviando} onClick={registrar}>
            Registrar carta firmada
          </Boton>
          <Boton variante="secundario" disabled={enviando} onClick={() => setAdjuntando(false)}>
            Cancelar
          </Boton>
        </>
      )}

      {fallo && <Aviso tipo="error" titulo={fallo} />}
    </Tarjeta>
  );
}