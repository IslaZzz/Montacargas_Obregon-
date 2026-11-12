import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Campo from "../../components/CampoForm";
import { useTitulo } from "../../hooks/useTitulo";
import { ErrorApi } from "../../services/apiClient";
import { formatoDiaCorto } from "../../utils/fechas";
import { confirmarRecoleccion, listarRentasSinRecoleccion } from "./rentasCapturaApi";
import type { RentaCreada } from "./rentasCapturaTypes";
import "../../styles/clientes/registrar-cliente.css";
import "../../styles/rentas-captura/registrar-renta.css";

/*
 * Horario de recolección de una renta en curso. Es temporal, igual que la captura de rentas:
 * no tiene opción en el menú, se entra escribiendo /rentas/recoleccion.
 * Al guardar nace el traslado de recolección por asignar.
 */
export default function ProgramarRecoleccion() {
  useTitulo("Programar recolección");

  const [rentas, setRentas] = useState<RentaCreada[]>([]);
  const [cargando, setCargando] = useState(true);
  const [rentaId, setRentaId] = useState("");
  const [dia, setDia] = useState("");
  const [desde, setDesde] = useState("08:00");
  const [hasta, setHasta] = useState("10:00");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [guardada, setGuardada] = useState<RentaCreada | null>(null);

  useEffect(() => {
    listarRentasSinRecoleccion()
      .then(setRentas)
      .catch(() => setError("No fue posible cargar las rentas. Revisa tu conexión."))
      .finally(() => setCargando(false));
  }, []);

  // Al elegir la renta se propone su último día
  function elegirRenta(id: string) {
    setRentaId(id);
    const renta = rentas.find((r) => String(r.id) === id);
    setDia(renta ? renta.fechaFin : "");
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault();
    setError("");
    setGuardada(null);

    if (!rentaId || !dia || !desde || !hasta) {
      setError("Elige la renta, el día y el horario.");
      return;
    }
    if (hasta <= desde) {
      setError("La hora de fin debe ser después de la de inicio.");
      return;
    }

    setGuardando(true);
    try {
      const renta = await confirmarRecoleccion(Number(rentaId), {
        desde: `${dia}T${desde}:00`,
        hasta: `${dia}T${hasta}:00`,
      });
      setGuardada(renta);
      setRentas((anteriores) => anteriores.filter((r) => r.id !== renta.id));
      setRentaId("");
      setDia("");
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : "No fue posible guardar. Revisa tu conexión.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="pagina-form" onSubmit={guardar} noValidate>
      <div className="pagina captura-renta captura-recoleccion">
        <h1>Programar recolección</h1>
        <p>Captura el horario que se acordó con el cliente para recoger los equipos.</p>

        {guardada && (
          <Aviso tipo="exito" titulo={`Recolección de ${guardada.folio} programada`} onCerrar={() => setGuardada(null)}>
            Ya puedes asignar su <Link to="/traslados">traslado de recolección</Link>.
          </Aviso>
        )}
        {error && <Aviso tipo="error" titulo={error} />}

        <div className="secciones">
          <section className="tarjeta">
            <label className="campo">
              <span className="campo-etiqueta">
                Renta en curso <small>(obligatorio)</small>
              </span>
              <select
                className="campo-input"
                value={rentaId}
                disabled={cargando}
                onChange={(e) => elegirRenta(e.target.value)}
              >
                <option value="">
                  {cargando
                    ? "Cargando rentas…"
                    : rentas.length === 0
                      ? "No hay rentas en curso sin recolección"
                      : "Elige una renta"}
                </option>
                {rentas.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.folio} · {r.cliente} · termina {formatoDiaCorto(r.fechaFin)} ({r.equipos.join(", ")})
                  </option>
                ))}
              </select>
            </label>

            <Campo etiqueta="Día" obligatorio type="date" value={dia} onChange={setDia} />
            <div className="fila-campos">
              <Campo etiqueta="Desde" obligatorio type="time" value={desde} onChange={setDesde} />
              <Campo etiqueta="Hasta" obligatorio type="time" value={hasta} onChange={setHasta} />
            </div>
          </section>
        </div>
      </div>

      <div className="barra-acciones">
        <p>Al guardar se crea el traslado de recolección por asignar.</p>
        <Button type="submit" variant="primary" disabled={guardando}>
          {guardando ? "Guardando…" : "Guardar recolección"}
        </Button>
      </div>
    </form>
  );
}