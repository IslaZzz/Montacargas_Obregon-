import { useId, useRef, useState } from "react";
import type { DragEvent } from "react";
import Aviso from "../../components/Aviso";
import Modal from "../../components/Modal";
import type { DatosFirma } from "./cartasApi";
import type { Carta, TipoFirma } from "./cartasTypes";
import { ahoraParaInput, MAXIMO_ARCHIVO_FIRMADO, textoTamano, TIPOS_FIRMA } from "./cartasUtils";

interface ModalRegistrarFirmaProps {
  carta: Carta;
  registrando: boolean;
  error: string; // Lo que respondió el backend si no la registró
  onCancelar: () => void;
  onRegistrar: (datos: DatosFirma) => void;
}

const icono = (nombre: string) => (
  <span className="material-symbols-rounded" aria-hidden="true">
    {nombre}
  </span>
);

// Por qué no sirve el archivo para ese tipo de firma; vacío si sirve
function problemaArchivo(tipo: TipoFirma, archivo: File) {
  if (!(TIPOS_FIRMA[tipo] as readonly string[]).includes(archivo.type)) {
    return tipo === "ELECTRONICA"
      ? "La carta firmada electrónicamente debe ser un PDF."
      : "El archivo debe ser PDF, JPG o PNG.";
  }
  if (archivo.size > MAXIMO_ARCHIVO_FIRMADO) return "El archivo no puede pesar más de 10 MB.";
  return "";
}

// "Registrar firma": tipo, fecha, quién firma y la carta firmada (Figma OHU-05)
export function ModalRegistrarFirma({ carta, registrando, error, onCancelar, onRegistrar }: ModalRegistrarFirmaProps) {
  const [tipo, setTipo] = useState<TipoFirma>(carta.estado === "ENVIADA" ? "ELECTRONICA" : "FISICA");
  const [fecha, setFecha] = useState(ahoraParaInput);
  const [firmante, setFirmante] = useState(carta.firmanteSugerido);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [errores, setErrores] = useState<{ fecha?: string; firmante?: string; archivo?: string }>({});
  const [arrastrando, setArrastrando] = useState(false);
  const entrada = useRef<HTMLInputElement>(null);
  const ids = { fecha: useId(), firmante: useId(), archivo: useId() };

  const electronica = tipo === "ELECTRONICA";

  const elegirArchivo = (nuevo: File | undefined) => {
    if (!nuevo) return;
    setArchivo(nuevo);
    setErrores((e) => ({ ...e, archivo: problemaArchivo(tipo, nuevo) }));
  };

  const cambiarTipo = (nuevo: TipoFirma) => {
    setTipo(nuevo);
    // Una foto no sirve para la firma electrónica: se quita para elegir el PDF
    if (archivo && problemaArchivo(nuevo, archivo)) setArchivo(null);
    setErrores((e) => ({ ...e, archivo: "" }));
  };

  const soltar = (e: DragEvent) => {
    e.preventDefault();
    setArrastrando(false);
    elegirArchivo(e.dataTransfer.files[0]);
  };

  const registrar = () => {
    const nuevos = {
      fecha: !fecha
        ? "Indica la fecha y hora de la firma."
        : new Date(fecha).getTime() > Date.now() + 5 * 60_000
          ? "La fecha de la firma no puede ser posterior a hoy."
          : "",
      firmante: firmante.trim() ? "" : "Escribe el nombre de quien firma.",
      archivo: archivo
        ? problemaArchivo(tipo, archivo)
        : electronica
          ? "Adjunta la carta firmada que regresó el cliente (PDF)."
          : "Adjunta la carta firmada para registrar la firma.",
    };
    setErrores(nuevos);
    if (nuevos.fecha || nuevos.firmante || nuevos.archivo || !archivo) return;
    onRegistrar({ tipo, fecha, firmante: firmante.trim(), archivo });
  };

  const textoErrorArchivo = errores.archivo ?? "";

  return (
    <Modal
      abierto
      titulo="Registrar firma"
      subtitulo={`Renta ${carta.folio}`}
      onCerrar={onCancelar}
      acciones={
        <>
          <button type="button" className="btn btn-tertiary carta-boton-modal" onClick={onCancelar}>
            Cancelar
          </button>
          <button
            type="button"
            className="btn btn-primary carta-boton-modal"
            onClick={registrar}
            disabled={registrando}
          >
            {icono("check")}
            {registrando ? "Registrando…" : "Registrar firma"}
          </button>
        </>
      }
    >
      <div className="carta-firma">
        <div className="carta-firma-tipos" role="radiogroup" aria-label="Tipo de firma">
          {(["ELECTRONICA", "FISICA"] as const).map((t) => (
            <button
              key={t}
              type="button"
              role="radio"
              aria-checked={tipo === t}
              className={`carta-firma-tipo${tipo === t ? " carta-firma-tipo-activo" : ""}`}
              onClick={() => cambiarTipo(t)}
            >
              {icono(t === "ELECTRONICA" ? "draw" : "description")}
              {t === "ELECTRONICA" ? "Firma electrónica" : "Firma física"}
            </button>
          ))}
        </div>

        {error && <Aviso tipo="error" titulo={error} />}

        <label htmlFor={ids.fecha}>Fecha y hora de la firma</label>
        <input
          id={ids.fecha}
          type="datetime-local"
          value={fecha}
          max={ahoraParaInput()}
          onChange={(e) => setFecha(e.target.value)}
          aria-invalid={!!errores.fecha}
        />
        {errores.fecha && <p className="carta-firma-error">{icono("error")}{errores.fecha}</p>}

        <label htmlFor={ids.firmante}>Nombre de quien firma</label>
        <input
          id={ids.firmante}
          type="text"
          maxLength={150}
          value={firmante}
          onChange={(e) => setFirmante(e.target.value)}
          aria-invalid={!!errores.firmante}
        />
        {errores.firmante && <p className="carta-firma-error">{icono("error")}{errores.firmante}</p>}

        <label htmlFor={ids.archivo}>
          {electronica ? "Carta firmada recibida (PDF)" : "Foto o escaneo de la carta firmada"}
        </label>
        <input
          ref={entrada}
          id={ids.archivo}
          type="file"
          className="carta-firma-entrada"
          accept={TIPOS_FIRMA[tipo].join(",")}
          onChange={(e) => {
            elegirArchivo(e.target.files?.[0]);
            e.target.value = ""; // Para poder volver a elegir el mismo archivo
          }}
        />
        {archivo ? (
          <div className={`carta-firma-archivo${textoErrorArchivo ? " carta-firma-archivo-error" : ""}`}>
            {icono(archivo.type.startsWith("image/") ? "image" : "picture_as_pdf")}
            <div>
              <p>{archivo.name}</p>
              <span>
                {archivo.type === "application/pdf" ? "PDF" : archivo.type === "image/png" ? "PNG" : "JPG"} de{" "}
                {textoTamano(archivo.size)}
              </span>
            </div>
            {!textoErrorArchivo && <span className="carta-firma-ok">{icono("check_circle")}</span>}
            <button
              type="button"
              className="carta-firma-quitar"
              aria-label="Quitar archivo"
              onClick={() => {
                setArchivo(null);
                setErrores((e) => ({ ...e, archivo: "" }));
              }}
            >
              {icono("close")}
            </button>
          </div>
        ) : (
          <button
            type="button"
            className={`carta-firma-zona${textoErrorArchivo ? " carta-firma-zona-error" : ""}${arrastrando ? " carta-firma-zona-encima" : ""}`}
            onClick={() => entrada.current?.click()}
            onDragOver={(e) => {
              e.preventDefault();
              setArrastrando(true);
            }}
            onDragLeave={() => setArrastrando(false)}
            onDrop={soltar}
          >
            {icono("upload_file")}
            <strong>Arrastra el archivo o selecciónalo</strong>
            <span>{electronica ? "PDF, máximo 10 MB" : "PDF, JPG o PNG, máximo 10 MB"}</span>
          </button>
        )}
        {textoErrorArchivo && <p className="carta-firma-error">{icono("error")}{textoErrorArchivo}</p>}
      </div>
    </Modal>
  );
}
