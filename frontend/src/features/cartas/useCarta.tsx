import { useCallback, useEffect, useState } from "react";
import { ErrorApi } from "../../services/apiClient";
import { generarCarta, obtenerCarta } from "./cartasApi";
import type { Carta, DatosFaltantes } from "./cartasTypes";
import { ModalDatosFaltantes, ModalRegenerar } from "./ModalesCarta";

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
