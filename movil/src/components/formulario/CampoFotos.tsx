import { useState } from "react";
import { mensajeDeError } from "../../services/api";
import type { Foto } from "../../services/fotosApi";
import Icono from "../Icono";
import Pregunta from "./Pregunta";

interface Props {
  etiqueta: string; // "Fotos", "Evidencia fotográfica"
  requeridas: string[]; // Una casilla por foto: ["Frente", "Lateral", "Tablero y horómetro", "Llantas"]
  subidas: Record<string, Foto | undefined>; // Las que ya se subieron, por etiqueta
  onSubir: (etiqueta: string, archivo: File) => Promise<void>; // Normalmente llama a subirFoto(...)
  obligatorio?: boolean;
  error?: string;
}

/**
 * Casillas para tomar fotos con la cámara del celular. Cada foto se sube al tomarla;
 * tocar una casilla con foto la vuelve a tomar.
 */
export default function CampoFotos({ etiqueta, requeridas, subidas, onSubir, obligatorio, error }: Props) {
  const [subiendo, setSubiendo] = useState<string | null>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});

  async function elegir(nombre: string, archivo: File | undefined) {
    if (!archivo) return;
    setSubiendo(nombre);
    setErrores((e) => ({ ...e, [nombre]: "" }));
    try {
      await onSubir(nombre, archivo);
    } catch (err) {
      setErrores((e) => ({ ...e, [nombre]: mensajeDeError(err, "No se pudo subir la foto.") }));
    } finally {
      setSubiendo(null);
    }
  }

  const fallo = requeridas.map((n) => errores[n]).find(Boolean);

  return (
    <Pregunta etiqueta={etiqueta} obligatorio={obligatorio} error={error ?? fallo}>
      <div className="fotos">
        {requeridas.map((nombre) => {
          const foto = subidas[nombre];
          return (
            <label key={nombre} className={`foto${foto ? " foto-lista" : ""}${errores[nombre] ? " foto-error" : ""}`}>
              <input
                type="file"
                accept="image/*"
                capture="environment"
                disabled={subiendo !== null}
                onChange={(e) => {
                  elegir(nombre, e.target.files?.[0]);
                  e.target.value = ""; // Para poder volver a tomar la misma
                }}
              />
              {foto && <img src={foto.url} alt="" />}
              <span className="foto-estado">
                <Icono
                  nombre={subiendo === nombre ? "progress_activity" : foto ? "check_circle" : "add_a_photo"}
                  className={subiendo === nombre ? "girando" : ""}
                />
              </span>
              <span className="foto-nombre">{subiendo === nombre ? "Subiendo…" : nombre}</span>
            </label>
          );
        })}
      </div>
    </Pregunta>
  );
}