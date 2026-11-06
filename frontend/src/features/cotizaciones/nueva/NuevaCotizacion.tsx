import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../components/buttons";
import { useTitulo } from "../../../hooks/useTitulo";
import { ErrorApi } from "../../../services/apiClient";
import { crearCotizacion } from "./nuevaCotizacionApi";
import { borradorVacio } from "./nuevaCotizacionTypes";
import type { BorradorCotizacion, CrearCotizacionRequest, Errores } from "./nuevaCotizacionTypes";
import { calcularDias, calcularTotales, enPesos, fechaHoraIso, importeCentavos } from "./calculo";
import { erroresDelBackend, hayErrores, pasoConErrores, validarPaso } from "./validacion";
import PasoCliente from "./PasoCliente";
import PasoFechasObra from "./PasoFechasObra";
import PasoEquipos from "./PasoEquipos";
import PasoRevisar from "./PasoRevisar";
import "../../../styles/cotizaciones/cotizaciones.css";
import "../../../styles/cotizaciones/nueva-cotizacion.css";

const PASOS = ["Cliente", "Fechas y obra", "Equipos", "Revisar y guardar"];
const ULTIMO_PASO = PASOS.length - 1;
const PASO_EQUIPOS = 2;

function aSolicitud(b: BorradorCotizacion): CrearCotizacionRequest | null {
  if (!b.cliente || b.razonSocialId === null) return null;
  return {
    clienteId: b.cliente.id,
    razonSocialId: b.razonSocialId,
    fechaEntrega: fechaHoraIso(b.fechaEntrega, b.horaEntrega),
    fechaRecoleccion: fechaHoraIso(b.fechaRecoleccion, b.horaRecoleccion),
    direccionObra: b.direccionObra.trim(),
    contactoObra: b.contactoObra.trim(),
    telefonoContacto: b.telefonoContacto,
    equipoIds: b.equipos.map((e) => e.id),
  };
}

// En celular, la barra de abajo del paso de equipos muestra cuántos van y el subtotal (nueva-cotizacion.css)
function resumirEquipos(b: BorradorCotizacion) {
  const cuantos = b.equipos.length;
  if (cuantos === 0) return null;
  const dias = calcularDias(b.fechaEntrega, b.fechaRecoleccion) ?? 0;
  const { subtotal } = calcularTotales(b.equipos.map((e) => importeCentavos(dias, e.modelo.tarifaDia)));
  return { texto: `${cuantos} ${cuantos === 1 ? "equipo" : "equipos"}`, subtotal: enPesos(subtotal) };
}

const textoDeErrores = (total: number) =>
  `Hay ${total} ${total === 1 ? "dato" : "datos"} por corregir`;

export default function NuevaCotizacion() {
  useTitulo("Nueva cotización");
  const navigate = useNavigate();

  const [paso, setPaso] = useState(0);
  const [borrador, setBorrador] = useState<BorradorCotizacion>(borradorVacio);
  const [errores, setErrores] = useState<Errores>({});
  const [errorGuardar, setErrorGuardar] = useState("");
  const [guardando, setGuardando] = useState(false);

  const cambiar = (cambios: Partial<BorradorCotizacion>) => {
    setBorrador((actual) => ({ ...actual, ...cambios }));
    setErrores((actuales) => {
      const restantes = { ...actuales };
      for (const campo of Object.keys(cambios) as (keyof BorradorCotizacion)[]) delete restantes[campo];
      return restantes;
    });
  };

  const irAPaso = (destino: number) => {
    setPaso(destino);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const siguiente = () => {
    const encontrados = validarPaso(paso, borrador);
    setErrores(encontrados);
    if (!hayErrores(encontrados)) irAPaso(paso + 1);
  };

  const anterior = () => {
    setErrores({});
    setErrorGuardar("");
    if (paso === 0) navigate("/cotizaciones");
    else irAPaso(paso - 1);
  };

  const guardar = async () => {
    const solicitud = aSolicitud(borrador);
    if (!solicitud) return irAPaso(0);
    setGuardando(true);
    setErrorGuardar("");
    try {
      const cotizacion = await crearCotizacion(solicitud);
      navigate(`/cotizaciones/${cotizacion.id}`, { state: { creada: cotizacion.folio } });
    } catch (error) {
      const delBackend = error instanceof ErrorApi ? erroresDelBackend(error.errores) : {};
      const pasoAfectado = pasoConErrores(delBackend);
      setErrores(delBackend);
      setErrorGuardar(error instanceof Error ? error.message : "No se pudo guardar la cotización.");
      if (pasoAfectado !== null) irAPaso(pasoAfectado);
      setGuardando(false);
    }
  };

  const totalErrores = Object.keys(errores).length;
  const resumenEquipos = paso === PASO_EQUIPOS ? resumirEquipos(borrador) : null;

  return (
    <div className="pagina-form">
      <div className="pagina nueva-cotizacion">
        <h1>Nueva cotización</h1>
        <p>Sigue los 4 pasos. Puedes regresar a cualquier paso antes de guardar.</p>

        <ol className="nc-pasos">
          {PASOS.map((nombre, i) => (
            <li key={nombre} className={i === paso ? "activo" : i < paso ? "hecho" : undefined}
              aria-current={i === paso ? "step" : undefined}>
              <span className="nc-pasos-numero">{i < paso ? "✓" : i + 1}</span>
              {nombre}
            </li>
          ))}
        </ol>

        {(totalErrores > 0 || errorGuardar) && (
          <div className="aviso-error" role="alert">
            <span className="material-symbols-rounded">error</span>
            <div>
              <strong>{errorGuardar || textoDeErrores(totalErrores)}</strong>
              <p>
                {totalErrores > 0
                  ? (errores.cliente ?? errores.razonSocialId ?? errores.equipos ?? "Revisa los campos marcados en rojo.")
                  : "Intenta de nuevo. Si sigue fallando, revisa que el servidor esté encendido."}
              </p>
            </div>
          </div>
        )}

        {paso === 0 && <PasoCliente borrador={borrador} cambiar={cambiar} />}
        {paso === 1 && <PasoFechasObra borrador={borrador} cambiar={cambiar} errores={errores} />}
        {paso === 2 && <PasoEquipos borrador={borrador} cambiar={cambiar} />}
        {paso === 3 && <PasoRevisar borrador={borrador} irAPaso={irAPaso} />}
      </div>

      <div className="barra-acciones">
        <p className={resumenEquipos ? "nc-barra-con-resumen" : undefined}>
          <span>
            Paso {paso + 1} de {PASOS.length} ·{" "}
            <span className="nc-barra-paso">{PASOS[paso]}</span>
            {resumenEquipos && <span className="nc-barra-resumen">{resumenEquipos.texto}</span>}
          </span>
          {resumenEquipos && <strong className="nc-barra-resumen">{resumenEquipos.subtotal}</strong>}
        </p>
        <Button type="button" variant="secondary" disabled={guardando} onClick={anterior}>
          {paso === 0 ? "Cancelar" : "Anterior"}
        </Button>
        {paso < ULTIMO_PASO ? (
          <Button type="button" variant="primary" onClick={siguiente}>Siguiente</Button>
        ) : (
          <Button type="button" variant="primary" disabled={guardando} onClick={guardar}>
            {guardando ? "Guardando…" : "Guardar cotización"}
          </Button>
        )}
      </div>
    </div>
  );
}
