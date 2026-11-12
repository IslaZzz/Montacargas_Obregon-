import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../../components/buttons'
import DataTable, { type TableColumn } from '../../../components/table'
import DateRangePicker from '../../../components/filters/dateRangePicker'
import FilterDropdown from '../../../components/filters/filterDropdown'
import type { DateRangeValue } from '../../../components/filters/filterTypes'
import { useTitulo } from '../../../hooks/useTitulo'
import { consultarDisponibilidad, listarCategorias } from './disponibilidadApi'
import type { Categoria, Disponibilidad as Datos, EquipoDisponible, Etiqueta } from './disponibilidadApi'

const ETIQUETAS: { valor: Etiqueta; texto: string }[] = [
  { valor: 'APTO_URGENCIAS', texto: 'Apto para urgencias' },
  { valor: 'REQUIERE_OPERADOR', texto: 'Requiere operador' },
  { valor: 'USO_INTERIORES', texto: 'Uso en interiores' },
]

const textoEtiqueta = (e: Etiqueta) => ETIQUETAS.find((x) => x.valor === e)?.texto ?? e

const pesos = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' })

// 2026-10-12 pasa a una fecha local, sin desfase de zona horaria
const aFecha = (iso: string) => {
  const [anio, mes, dia] = iso.split('-').map(Number)
  return new Date(anio, mes - 1, dia)
}

// Del lunes 12 al viernes 16 de octubre de 2026
const rangoLargo = (desde: string, hasta: string) => {
  const d = aFecha(desde)
  const h = aFecha(hasta)
  const inicio = d.toLocaleDateString('es-MX', {
    weekday: 'long',
    day: 'numeric',
    ...(d.getMonth() === h.getMonth() ? {} : { month: 'long' }),
  })
  const fin = h.toLocaleDateString('es-MX', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
  return `Del ${inicio} al ${fin}`
}

// del 12 al 16 de octubre
const rangoCorto = (desde: string, hasta: string) => {
  const d = aFecha(desde)
  const h = aFecha(hasta)
  const mes = (f: Date) => f.toLocaleDateString('es-MX', { month: 'long' })
  return d.getMonth() === h.getMonth()
    ? `del ${d.getDate()} al ${h.getDate()} de ${mes(h)}`
    : `del ${d.getDate()} de ${mes(d)} al ${h.getDate()} de ${mes(h)}`
}

// Lo que se consultó, para mostrarlo en el resumen
interface Resultado extends Datos {
  categoria: string
  etiqueta: Etiqueta | ''
  desde: string
  hasta: string
}

export default function Disponibilidad() {
  useTitulo('Disponibilidad')
  const navigate = useNavigate()

  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [categoriaId, setCategoriaId] = useState('')
  const [etiqueta, setEtiqueta] = useState<Etiqueta | ''>('')
  const [fechas, setFechas] = useState<DateRangeValue>({ from: '', to: '' })

  const [aviso, setAviso] = useState('')
  const [cargando, setCargando] = useState(false)
  const [resultado, setResultado] = useState<Resultado | null>(null)

  useEffect(() => {
    // Se cargan las categorías al abrir la pantalla
    listarCategorias()
      .then(setCategorias)
      .catch(() => setAviso('No se pudieron cargar las categorías. Revisa que el servidor esté encendido.'))
  }, [])

  const nombreCategoria = categorias.find((c) => String(c.id) === categoriaId)?.nombre ?? ''

  const buscar = async (etiquetaUsada: Etiqueta | '' = etiqueta) => {
    if (!categoriaId) {
      setAviso('Elige categoría para buscar')
      return
    }
    
    if (!fechas.from || !fechas.to) {
      setAviso('Elige fechas (inicio y fin) para buscar')
      return
    }

    setAviso('')
    setCargando(true)
    try {
      const datos = await consultarDisponibilidad(Number(categoriaId), etiquetaUsada, fechas.from, fechas.to)
      setEtiqueta(etiquetaUsada)
      setResultado({
        ...datos,
        categoria: nombreCategoria,
        etiqueta: etiquetaUsada,
        desde: fechas.from,
        hasta: fechas.to,
      })
    } catch {
      setAviso('No se pudo consultar. Revisa que el servidor esté encendido.')
    } finally {
      setCargando(false)
    }
  }

  // Columnas de la tabla de resultados
  const columnas: TableColumn<EquipoDisponible>[] = [
    {
      header: 'Unidad y equipo',
      accessor: (e) => (
        <>
          <span className="cliente-nombre">{e.numeroUnidad}</span>
          <span className="cliente-empresa">{e.modelo}</span>
        </>
      ),
    },
    {
      header: 'Etiquetas',
      accessor: (e) =>
        e.etiquetas.length === 0
          ? '—'
          : e.etiquetas.map((t) => (
              <span className="etiqueta-chip" key={t}>
                <span className="material-symbols-rounded">sell</span>
                {textoEtiqueta(t)}
              </span>
            )),
    },
    {
      header: 'Tarifa vigente',
      accessor: (e) => <strong>{pesos.format(e.tarifaDia)} por día</strong>,
    },
  ]

  return (
    <div className="pagina disponibilidad">
      <div className="encabezado-pagina">
        <h1>Disponibilidad</h1>
        <p>Consulta qué equipos se pueden rentar según la categoría y el rango de fechas.</p>
      </div>

      {aviso && (
        <div className="aviso-error" role="alert">
          <span className="material-symbols-rounded">error</span>
          <strong>{aviso}</strong>
        </div>
      )}

      {/* Formulario de búsqueda */}
      {!resultado && (
        <>
          <div className="aviso-info">
            <span className="material-symbols-rounded">info</span>
            <div>
              <strong>Consultar no aparta el equipo</strong>
              <p>Solo muestra lo que está libre en este momento; no bloquea las fechas para nadie.</p>
            </div>
          </div>

          <div className="filtros-disponibilidad">
            <section className="tarjeta">
              <h2>
                <span className="paso">1</span> Qué equipo buscas
              </h2>

              <div className="campo-bloque">
                <span className="campo-etiqueta">Categoría (obligatorio)</span>
                <FilterDropdown
                  options={categorias.map((c) => ({ label: c.nombre, value: String(c.id) }))}
                  selectedValue={categoriaId || 'all'}
                  allLabel="Elige una categoría"
                  onChange={(valor) => setCategoriaId(valor === 'all' ? '' : valor)}
                />
              </div>

              <div className="campo-bloque">
                <span className="campo-etiqueta">Etiqueta (opcional)</span>
                <div className="filters-row">
                  {ETIQUETAS.map((t) => (
                    <button
                      type="button"
                      key={t.valor}
                      className={etiqueta === t.valor ? 'filter-chip active' : 'filter-chip'}
                      onClick={() => setEtiqueta(etiqueta === t.valor ? '' : t.valor)}
                    >
                      {t.texto}
                    </button>
                  ))}
                </div>
                <p className="nota">
                  Toca una etiqueta solo si el cliente necesita algo específico. Puedes dejarla vacía.
                </p>
              </div>
            </section>

            <section className="tarjeta">
              <h2>
                <span className="paso">2</span> Fechas de la renta
              </h2>
              <DateRangePicker value={fechas} onChange={(campo, valor) => setFechas({ ...fechas, [campo]: valor })} />
              <p className="nota">Se deja 1 día libre antes y después de cada renta.</p>
            </section>
          </div>

          <Button type="button" variant="primary" className="btn-buscar" disabled={cargando} onClick={() => buscar()}>
            <span className="material-symbols-rounded">search</span>
            {cargando ? 'Consultando…' : 'Consultar disponibilidad'}
          </Button>
        </>
      )}

      {/* Resultados */}
      {resultado && (
        <>
          <section className="resumen-consulta">
            <div>
              <span>Categoría</span>
              <strong>{resultado.categoria}</strong>
            </div>
            <div>
              <span>Etiqueta</span>
              <strong>{resultado.etiqueta ? textoEtiqueta(resultado.etiqueta) : 'Ninguna'}</strong>
            </div>
            <div>
              <span>Fechas</span>
              <strong>{rangoLargo(resultado.desde, resultado.hasta)}</strong>
            </div>
            <Button type="button" variant="secondary" onClick={() => setResultado(null)}>
              <span className="material-symbols-rounded">edit</span>
              Cambiar búsqueda
            </Button>
          </section>

          {resultado.equipos.length > 0 ? (
            <>
              <div className="aviso-exito" role="status">
                <span className="material-symbols-rounded">check_circle</span>
                <div>
                  <strong>
                    {resultado.equipos.length}{' '}
                    {resultado.equipos.length === 1 ? 'equipo disponible' : 'equipos disponibles'}{' '}
                    {rangoCorto(resultado.desde, resultado.hasta)}
                  </strong>
                  {resultado.ocupados > 0 && (
                    <p>
                      {resultado.ocupados === 1
                        ? 'Otro equipo no aparece porque ya está rentado o comprometido en esas fechas.'
                        : `Otros ${resultado.ocupados} no aparecen porque ya están rentados o comprometidos en esas fechas.`}
                    </p>
                  )}
                </div>
              </div>

              <div className="tabla-disponibilidad">
                <DataTable columns={columnas} data={resultado.equipos} getRowKey={(e) => e.id} />
              </div>

              <div className="pie-disponibilidad">
                <span className="material-symbols-rounded">info</span>
                <div>
                  <strong>Consultar no aparta el equipo</strong>
                  <p>Si el cliente quiere rentarlo, crea la cotización con estas fechas.</p>
                </div>
                <Button
                  type="button"
                  variant="primary"
                  onClick={() =>
                    navigate('/cotizaciones/nueva', { state: { fechas: { desde: resultado.desde, hasta: resultado.hasta } } })
                  }
                >
                  Crear cotización →
                </Button>
              </div>
            </>
          ) : (
            <>
              <div className="aviso-advertencia" role="status">
                <span className="material-symbols-rounded">warning</span>
                <div>
                  <strong>No hay equipos disponibles en esas fechas</strong>
                  <p>Prueba alguna de estas opciones:</p>
                </div>
              </div>

              <section className="tarjeta">
                <h2>Qué puedes hacer</h2>

                {resultado.etiqueta && (
                  <div className="opcion-disponibilidad">
                    <p>Quitar la etiqueta “{textoEtiqueta(resultado.etiqueta)}”</p>
                    <Button type="button" variant="primary" disabled={cargando} onClick={() => buscar('')}>
                      Quitar etiqueta
                    </Button>
                  </div>
                )}

                <div className="opcion-disponibilidad">
                  <p>Buscar en otras fechas</p>
                  <Button type="button" variant="secondary" onClick={() => setResultado(null)}>
                    Cambiar fechas
                  </Button>
                </div>

                <div className="opcion-disponibilidad">
                  <p>Revisar otra categoría</p>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      setCategoriaId('')
                      setResultado(null)
                    }}
                  >
                    Ver todas las categorías
                  </Button>
                </div>
              </section>
            </>
          )}
        </>
      )}
    </div>
  )
}