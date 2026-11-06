import { useCallback, useEffect, useState } from "react";
import { ErrorApi } from "../../services/apiClient";
import { enviarCarta, generarCarta, obtenerCarta, registrarFirma } from "./cartasApi";
import type { DatosFirma } from "./cartasApi";
import type { Carta, DatosFaltantes } from "./cartasTypes";
import { ModalDatosFaltantes, ModalEnviar, ModalRegenerar } from "./ModalesCarta";
import { ModalRegistrarFirma } from "./ModalFirma";

// Carga la carta de una renta
export function useCarta(rentaId: string | undefined) {
  const [carta, setCarta] = useState<Carta | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!rentaId) return;
    let activo = true;
    obtenerCarta(rentaId)
      .then((c) => activo && setCarta(c))
      .catch(() => activo && setError("No fue posible cargar la carta de la renta."));
    return () => {
      activo = false;
    };
  }, [rentaId]);

  return { carta, setCarta, error };
}

// Generar y regenerar la carta, con sus modales.
// Quien lo usa pinta `modales` y recibe la carta nueva en onActualizada.
export function useGenerarCarta(carta: Carta | null, onActualizada: (carta: Carta) => void) {
  const [generando, setGenerando] = useState(false);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [faltantes, setFaltantes] = useState<DatosFaltantes | null>(null);
  const [confirmando, setConfirmando] = useState(false);

  const generar = useCallback(async () => {
    if (!carta) return;
    setGenerando(true);
    setError("");
    setExito("");
    try {
      const nueva = await generarCarta(carta.rentaId);
      onActualizada(nueva);
      setExito(
        nueva.version === 1
          ? "Carta generada. Revísala antes de enviarla."
          : `Carta regenerada: la v${nueva.version} es la vigente.`,
      );
    } catch (e) {
      // 422: faltan datos; se muestra cuáles en el modal
      if (e instanceof ErrorApi && Object.keys(e.errores).length > 0) {
        setFaltantes(e.errores);
      } else {
        setError(e instanceof Error ? e.message : "No fue posible generar la carta.");
      }
    } finally {
      setGenerando(false);
      setConfirmando(false);
    }
  }, [carta, onActualizada]);

  const cerrarFaltantes = useCallback(() => setFaltantes(null), []);
  const cancelar = useCallback(() => setConfirmando(false), []);

  const modales = (
    <>
      <ModalDatosFaltantes faltantes={faltantes} onCerrar={cerrarFaltantes} />
      {carta && (
        <ModalRegenerar
          carta={carta}
          abierto={confirmando}
          generando={generando}
          onCancelar={cancelar}
          onConfirmar={generar}
        />
      )}
    </>
  );

  return {
    generar,
    // Pide confirmación antes de reemplazar la versión vigente
    pedirRegenerar: () => setConfirmando(true),
    generando,
    error,
    exito,
    limpiarMensajes: () => {
      setError("");
      setExito("");
    },
    modales,
  };
}

// Enviar la carta al cliente, con su modal. Si el envío falla, `error` trae el motivo
// y `reintentar` lo vuelve a intentar con el mismo mensaje.
export function useEnviarCarta(carta: Carta | null, onActualizada: (carta: Carta) => void) {
  const [abierto, setAbierto] = useState(false);
  const [enviando, setEnviando] = useState(false);
  // El error es de una versión: si se regenera la carta, deja de mostrarse
  const [fallo, setFallo] = useState<{ texto: string; version: number | null } | null>(null);
  const [exito, setExito] = useState("");
  const [ultimoMensaje, setUltimoMensaje] = useState<string | null>(null);

  const enviar = useCallback(
    async (mensaje: string) => {
      if (!carta) return;
      setEnviando(true);
      setFallo(null);
      setExito("");
      setUltimoMensaje(mensaje);
      try {
        const nueva = await enviarCarta(carta.rentaId, mensaje);
        onActualizada(nueva);
        setExito(`Carta enviada a ${nueva.correoEnvio ?? nueva.correoCliente}.`);
      } catch (e) {
        setFallo({
          texto: e instanceof ErrorApi ? e.message : "No hubo conexión con el servidor.",
          version: carta.version,
        });
      } finally {
        setEnviando(false);
        setAbierto(false);
      }
    },
    [carta, onActualizada],
  );

  const cancelar = useCallback(() => setAbierto(false), []);
  const limpiarMensajes = useCallback(() => {
    setFallo(null);
    setExito("");
  }, []);

  const modal =
    carta && abierto ? (
      <ModalEnviar
        carta={carta}
        mensajeInicial={ultimoMensaje ?? carta.mensajeSugerido}
        enviando={enviando}
        onCancelar={cancelar}
        onEnviar={enviar}
      />
    ) : null;

  return {
    pedirEnvio: () => {
      limpiarMensajes();
      setAbierto(true);
    },
    reintentar: () => enviar(ultimoMensaje ?? carta?.mensajeSugerido ?? ""),
    enviando,
    error: fallo && carta && fallo.version === carta.version ? fallo.texto : "",
    exito,
    limpiarMensajes,
    modal,
  };
}

// Registrar la firma de la carta (OHU-05), con su modal. Si el backend la rechaza,
// el motivo se muestra dentro del modal y no se cierra.
export function useRegistrarFirma(carta: Carta | null, onActualizada: (carta: Carta) => void) {
  const [abierto, setAbierto] = useState(false);
  const [registrando, setRegistrando] = useState(false);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");

  const registrar = useCallback(
    async (datos: DatosFirma) => {
      if (!carta) return;
      setRegistrando(true);
      setError("");
      try {
        const nueva = await registrarFirma(carta.rentaId, datos);
        onActualizada(nueva);
        setAbierto(false);
        setExito("Firma registrada. La carta quedó como Firmada.");
      } catch (e) {
        setError(e instanceof ErrorApi ? e.message : "No hubo conexión con el servidor.");
      } finally {
        setRegistrando(false);
      }
    },
    [carta, onActualizada],
  );

  const cancelar = useCallback(() => {
    setAbierto(false);
    setError("");
  }, []);

  const modal =
    carta && abierto ? (
      <ModalRegistrarFirma
        carta={carta}
        registrando={registrando}
        error={error}
        onCancelar={cancelar}
        onRegistrar={registrar}
      />
    ) : null;

  return {
    pedirFirma: () => {
      setExito("");
      setAbierto(true);
    },
    exito,
    limpiarExito: () => setExito(""),
    modal,
  };
}
