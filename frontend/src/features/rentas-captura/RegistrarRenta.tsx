import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import Aviso from "../../components/Aviso";
import { Button } from "../../components/buttons";
import Campo from "../../components/CampoForm";
import FilterRow from "../../components/filters/filterRow";
import type { FilterOption } from "../../components/filters/filterTypes";
import { useTitulo } from "../../hooks/useTitulo";
import { ErrorApi } from "../../services/apiClient";
import { formatoMoneda } from "../../utils/formato";
import { buscarClientes } from "../clientes/clientesApi";
import type { Cliente } from "../clientes/clientesTypes";
import { crearRenta, listarEquiposDisponibles } from "./rentasCapturaApi";
import type { EquipoDisponible, RentaCreada } from "./rentasCapturaTypes";
import "../../styles/clientes/registrar-cliente.css";
import "../../styles/rentas-captura/registrar-renta.css";

/*
 * Captura manual de una renta. Es temporal: no tiene opción en el menú,
 * se entra escribiendo /rentas/nueva. Al guardar se confirma la entrega,
 * así nacen las revisiones de salida y el traslado de entrega.
 */

const formularioVacio = {
  clienteId: "",
  fechaInicio: "",
  fechaFin: "",
  direccionObra: "",
  contactoObra: "",
  telefonoContacto: "",
  fechaEntrega: "",
  entregaDesde: "08:00",
  entregaHasta: "10:00",
};

type Formulario = typeof formularioVacio;

// Deja solo dígitos y corta al máximo
const soloDigitos = (valor: string, max: number) =>
  valor.replace(/\D/g, "").slice(0, max);

// "Industrias Cajeme" o, si no tiene empresa, el nombre de la persona
const nombreCliente = (c: Cliente) =>
  c.empresa?.trim() ? c.empresa : `${c.nombre} ${c.apellidoPaterno}`;

export default function RegistrarRenta() {
  useTitulo("Registrar renta");
  const navigate = useNavigate();

  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [form, setForm] = useState<Formulario>(formularioVacio);

  // Equipos libres en las fechas elegidas y los que se marcaron
  const [equipos, setEquipos] = useState<EquipoDisponible[]>([]);
  const [cargandoEquipos, setCargandoEquipos] = useState(false);
  const [errorEquipos, setErrorEquipos] = useState("");
  const [seleccion, setSeleccion] = useState<Set<number>>(new Set());

  // Para encontrar equipos en la lista: categoría y texto
  const [categoria, setCategoria] = useState("all");
  const [busqueda, setBusqueda] = useState("");

  // True después de pulsar Guardar; los errores se muestran desde entonces
  const [enviado, setEnviado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [errorServidor, setErrorServidor] = useState("");
  const [creada, setCreada] = useState<RentaCreada | null>(null);

  // Las fechas ISO ("2026-11-10") se pueden comparar como texto
  const periodoValido =
    form.fechaInicio !== "" &&
    form.fechaFin !== "" &&
    form.fechaFin >= form.fechaInicio;

  useEffect(() => {
    // Se ejecuta una vez al abrir la pantalla
    buscarClientes()
      .then((lista) =>
        setClientes([...lista].sort((a, b) => nombreCliente(a).localeCompare(nombreCliente(b)))),
      )
      .catch(() => setErrorServidor("No fue posible cargar los clientes."));
  }, []);

  // Cada vez que cambian las fechas, trae los equipos libres en ese periodo
  useEffect(() => {
    if (!periodoValido) return;
    let vigente = true;
    setCargandoEquipos(true);
    setErrorEquipos("");
    listarEquiposDisponibles(form.fechaInicio, form.fechaFin)
      .then((lista) => {
        if (!vigente) return;
        setEquipos(lista);
        // Si un equipo marcado ya no está libre en las nuevas fechas, se desmarca
        setSeleccion((anterior) => new Set(lista.filter((e) => anterior.has(e.id)).map((e) => e.id)));
      })
      .catch(() => vigente && setErrorEquipos("No fue posible cargar los equipos disponibles."))
      .finally(() => vigente && setCargandoEquipos(false));
    return () => {
      vigente = false;
    };
  }, [form.fechaInicio, form.fechaFin, periodoValido]);

  function cambiar(campo: keyof Formulario, valor: string) {
    setForm((anterior) => {
      const nuevo = { ...anterior, [campo]: valor };
      // La entrega suele ser el primer día; se propone mientras no la cambien
      if (campo === "fechaInicio" && (anterior.fechaEntrega === "" || anterior.fechaEntrega === anterior.fechaInicio)) {
        nuevo.fechaEntrega = valor;
      }
      return nuevo;
    });
  }

  function marcar(id: number) {
    setSeleccion((anterior) => {
      const nueva = new Set(anterior);
      if (nueva.has(id)) nueva.delete(id);
      else nueva.add(id);
      return nueva;
    });
  }

  // Rangos al revés: se revisan mientras se escribe
  function validarRangos() {
    const e: Partial<Record<keyof Formulario | "equipos", string>> = {};
    if (form.fechaInicio && form.fechaFin && form.fechaFin < form.fechaInicio)
      e.fechaFin = "La fecha de fin no puede ser anterior a la de inicio.";
    if (form.fechaEntrega && form.fechaFin && form.fechaEntrega > form.fechaFin)
      e.fechaEntrega = "La entrega no puede ser después de que termine la renta.";
    if (form.entregaDesde && form.entregaHasta && form.entregaHasta <= form.entregaDesde)
      e.entregaHasta = "Debe ser después de la hora de inicio.";
    return e;
  }

  // Revisa los datos antes de enviarlos
  function validar() {
    const e: Partial<Record<keyof Formulario | "equipos", string>> = {};
    const vacio = (valor: string) => !valor.trim();

    if (vacio(form.clienteId)) e.clienteId = "Elige el cliente.";
    if (vacio(form.fechaInicio)) e.fechaInicio = "Elige la fecha de inicio.";
    if (vacio(form.fechaFin)) e.fechaFin = "Elige la fecha de fin.";
    else if (form.fechaInicio && form.fechaFin < form.fechaInicio)
      e.fechaFin = "La fecha de fin no puede ser anterior a la de inicio.";

    if (vacio(form.direccionObra)) e.direccionObra = "Escribe la dirección de la obra.";
    if (vacio(form.contactoObra)) e.contactoObra = "Escribe quién recibe en la obra.";
    if (form.telefonoContacto.length !== 10)
      e.telefonoContacto = "Escribe los 10 dígitos del teléfono.";

    if (seleccion.size === 0) e.equipos = "Elige al menos un equipo.";

    if (vacio(form.fechaEntrega)) e.fechaEntrega = "Elige el día de entrega.";
    else if (form.fechaFin && form.fechaEntrega > form.fechaFin)
      e.fechaEntrega = "La entrega no puede ser después de que termine la renta.";
    if (vacio(form.entregaDesde)) e.entregaDesde = "Elige la hora de inicio.";
    if (vacio(form.entregaHasta)) e.entregaHasta = "Elige la hora de fin.";
    else if (form.entregaDesde && form.entregaHasta <= form.entregaDesde)
      e.entregaHasta = "Debe ser después de la hora de inicio.";

    return e;
  }

  // Antes de guardar solo se avisa de los rangos al revés; al guardar, de todo
  const errores = enviado ? validar() : validarRangos();
  const totalErrores = enviado ? Object.keys(errores).length : 0;

  // Categorías de los equipos libres, con cuántos hay de cada una
  const categorias: FilterOption[] = [...new Set(equipos.map((e) => e.categoria))]
    .sort()
    .map((c) => ({ value: c, label: c, count: equipos.filter((e) => e.categoria === c).length }));

  // Equipos que se ven en la lista con el filtro y la búsqueda
  const texto = busqueda.trim().toLowerCase();
  const visibles = equipos.filter(
    (e) =>
      (categoria === "all" || e.categoria === categoria) &&
      (texto === "" ||
        e.numeroUnidad.toLowerCase().includes(texto) ||
        e.descripcion.toLowerCase().includes(texto)),
  );

  // Números de unidad marcados, aunque el filtro los esconda
  const marcados = equipos.filter((e) => seleccion.has(e.id)).map((e) => e.numeroUnidad);

  async function guardar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    setErrorServidor("");
    setCreada(null);

    if (Object.keys(validar()).length > 0) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    setGuardando(true);
    try {
      const renta = await crearRenta({
        clienteId: Number(form.clienteId),
        fechaInicio: form.fechaInicio,
        fechaFin: form.fechaFin,
        direccionObra: form.direccionObra.trim(),
        contactoObra: form.contactoObra.trim(),
        telefonoContacto: form.telefonoContacto,
        equipoIds: [...seleccion],
        entregaDesde: `${form.fechaEntrega}T${form.entregaDesde}:00`,
        entregaHasta: `${form.fechaEntrega}T${form.entregaHasta}:00`,
      });

      // Deja el formulario limpio para capturar otra
      setCreada(renta);
      setForm(formularioVacio);
      setEquipos([]);
      setSeleccion(new Set());
      setCategoria("all");
      setBusqueda("");
      setEnviado(false);
    } catch (err) {
      if (err instanceof ErrorApi) {
        const porCampo = Object.values(err.errores);
        setErrorServidor(porCampo.length > 0 ? `${err.message} ${porCampo.join(" ")}` : err.message);
      } else {
        setErrorServidor("No fue posible guardar la renta. Revisa tu conexión.");
      }
    } finally {
      setGuardando(false);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  }

  return (
    <form className="pagina-form" onSubmit={guardar} noValidate>
      <div className="pagina captura-renta">
        {/* Título */}
        <h1>Registrar renta</h1>
        <p>
          Captura la renta tal como quedó acordada con el cliente. Al guardarla se crean sus
          revisiones de salida y su traslado de entrega.
        </p>

        {/* Avisos */}
        {creada && (
          <Aviso tipo="exito" titulo={`Renta ${creada.folio} registrada`} onCerrar={() => setCreada(null)}>
            {creada.equipos.length} {creada.equipos.length === 1 ? "equipo" : "equipos"} para{" "}
            {creada.cliente} ({creada.equipos.join(", ")}). Ya puedes asignar sus{" "}
            <Link to="/revisiones">revisiones</Link> y su <Link to="/traslados">traslado</Link>.
          </Aviso>
        )}
        {totalErrores > 0 && (
          <Aviso
            tipo="error"
            titulo={`No se pudo guardar: hay ${totalErrores} ${totalErrores === 1 ? "dato" : "datos"} por corregir`}
          />
        )}
        {errorServidor && <Aviso tipo="error" titulo={errorServidor} />}

        <div className="secciones">
          {/* Cliente y periodo */}
          <section className="tarjeta">
            <h2>
              <span className="paso">1</span> Cliente y periodo
            </h2>

            <label className="campo">
              <span className="campo-etiqueta">
                Cliente <small>(obligatorio)</small>
              </span>
              <select
                className={"campo-input" + (errores.clienteId ? " con-error" : "")}
                value={form.clienteId}
                onChange={(e) => cambiar("clienteId", e.target.value)}
              >
                <option value="">Elige un cliente</option>
                {clientes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {nombreCliente(c)}
                  </option>
                ))}
              </select>
              {errores.clienteId && <span className="campo-error">{errores.clienteId}</span>}
            </label>

            <div className="fila-campos">
              <Campo
                etiqueta="Inicio"
                obligatorio
                type="date"
                value={form.fechaInicio}
                error={errores.fechaInicio}
                onChange={(v) => cambiar("fechaInicio", v)}
              />
              <Campo
                etiqueta="Fin"
                obligatorio
                type="date"
                min={form.fechaInicio || undefined}
                value={form.fechaFin}
                error={errores.fechaFin}
                onChange={(v) => cambiar("fechaFin", v)}
              />
            </div>
          </section>

          {/* Obra */}
          <section className="tarjeta">
            <h2>
              <span className="paso">2</span> Obra
            </h2>

            <Campo
              etiqueta="Dirección de la obra"
              obligatorio
              maxLength={200}
              value={form.direccionObra}
              error={errores.direccionObra}
              onChange={(v) => cambiar("direccionObra", v)}
            />
            <div className="fila-campos">
              <Campo
                etiqueta="Quién recibe"
                obligatorio
                maxLength={100}
                value={form.contactoObra}
                error={errores.contactoObra}
                onChange={(v) => cambiar("contactoObra", v)}
              />
              <Campo
                etiqueta="Teléfono"
                obligatorio
                inputMode="numeric"
                value={form.telefonoContacto}
                error={errores.telefonoContacto}
                onChange={(v) => cambiar("telefonoContacto", soloDigitos(v, 10))}
              />
            </div>
          </section>
        </div>

        <div className="secciones">
          {/* Equipos */}
          <section className="tarjeta">
            <h2>
              <span className="paso">3</span> Equipos
            </h2>

            {!periodoValido && (
              <p className="captura-renta-mensaje">Elige las fechas para ver los equipos libres.</p>
            )}
            {periodoValido && cargandoEquipos && (
              <p className="captura-renta-mensaje">Cargando equipos…</p>
            )}
            {periodoValido && errorEquipos && <p className="captura-renta-mensaje">{errorEquipos}</p>}
            {periodoValido && !cargandoEquipos && !errorEquipos && equipos.length === 0 && (
              <p className="captura-renta-mensaje">No hay equipos libres en esas fechas.</p>
            )}

            {periodoValido && equipos.length > 0 && (
              <>
                <input
                  type="search"
                  className="campo-input"
                  placeholder="Buscar por número de unidad, marca o modelo"
                  aria-label="Buscar equipo"
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                />
                <FilterRow
                  options={categorias}
                  selectedValue={categoria}
                  allLabel="Todas"
                  allCount={equipos.length}
                  onChange={setCategoria}
                />
                <p className="captura-renta-mensaje">
                  {seleccion.size === 0
                    ? "Ningún equipo seleccionado"
                    : `${seleccion.size} ${seleccion.size === 1 ? "seleccionado" : "seleccionados"}: ${marcados.join(", ")}`}
                </p>
                {visibles.length === 0 && (
                  <p className="captura-renta-mensaje">Ningún equipo coincide con la búsqueda.</p>
                )}
                <ul className="captura-renta-equipos">
                  {visibles.map((e) => (
                    <li key={e.id}>
                      <label className={"captura-renta-equipo" + (seleccion.has(e.id) ? " elegido" : "")}>
                        <input type="checkbox" checked={seleccion.has(e.id)} onChange={() => marcar(e.id)} />
                        <span className="captura-renta-equipo-texto">
                          <strong>{e.numeroUnidad}</strong>
                          {e.categoria} · {e.descripcion}
                        </span>
                        <span className="captura-renta-tarifa">{formatoMoneda(e.tarifaDia)} / día</span>
                      </label>
                    </li>
                  ))}
                </ul>
              </>
            )}

            {errores.equipos && <span className="campo-error">{errores.equipos}</span>}
          </section>

          {/* Horario de entrega */}
          <section className="tarjeta">
            <h2>
              <span className="paso">4</span> Horario de entrega
            </h2>
            <p className="ayuda">La ventana que se acordó con el cliente para llevar los equipos.</p>

            <Campo
              etiqueta="Día"
              obligatorio
              type="date"
              max={form.fechaFin || undefined}
              value={form.fechaEntrega}
              error={errores.fechaEntrega}
              onChange={(v) => cambiar("fechaEntrega", v)}
            />
            <div className="fila-campos">
              <Campo
                etiqueta="Desde"
                obligatorio
                type="time"
                value={form.entregaDesde}
                error={errores.entregaDesde}
                onChange={(v) => cambiar("entregaDesde", v)}
              />
              <Campo
                etiqueta="Hasta"
                obligatorio
                type="time"
                value={form.entregaHasta}
                error={errores.entregaHasta}
                onChange={(v) => cambiar("entregaHasta", v)}
              />
            </div>
          </section>
        </div>
      </div>

      {/* Acciones */}
      <div className="barra-acciones">
        <p>Revisa los datos antes de guardar.</p>
        <Button type="button" variant="tertiary" onClick={() => navigate(-1)} disabled={guardando}>
          Cancelar
        </Button>
        <Button type="submit" variant="primary" disabled={guardando}>
          {guardando ? "Guardando…" : "Guardar renta"}
        </Button>
      </div>
    </form>
  );
}