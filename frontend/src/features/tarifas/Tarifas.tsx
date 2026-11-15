import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../../components/buttons'
import DataTable, { type TableColumn } from '../../components/table'
import Aviso from '../../components/Aviso'
import { useTitulo } from '../../hooks/useTitulo'
import { listarTarifas } from './tarifasApi'
import type { Tarifa } from './tarifasTypes'
import { formatearPesos } from './tarifasUtils'
import EditarTarifaModal from './EditarTarifaModal'
import "../../styles/tarifas/tarifas.css";

// Valor del filtro cuando no se elige categoría
const TODAS = 'todas'

// Pantalla «Tarifas» (HU-04). Es una subpantalla de «Modelos».
export default function Tarifas() {
  useTitulo('Tarifas')

  const [tarifas, setTarifas] = useState<Tarifa[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  // Id de la categoría elegida, o "todas"
  const [categoriaId, setCategoriaId] = useState(TODAS)

  // Modelo que se está editando; null con el modal cerrado
  const [editando, setEditando] = useState<Tarifa | null>(null)

  // Última tarifa guardada: muestra el aviso y resalta su fila
  const [avisoDe, setAvisoDe] = useState<Tarifa | null>(null)
  const [idActualizado, setIdActualizado] = useState<number | null>(null)

  // Carga las tarifas al abrir la pantalla
  useEffect(() => {
    listarTarifas()
      .then(setTarifas)
      .catch(() => setError('No fue posible cargar las tarifas.'))
      .finally(() => setCargando(false))
  }, [])

  // Categorías para el filtro, sacadas de los mismos modelos y en orden alfabético
  const categorias = useMemo(() => {
    const mapa = new Map<number, string>()
    tarifas.forEach((t) => mapa.set(t.categoriaId, t.categoria))
    return [...mapa]
      .map(([id, nombre]) => ({ id, nombre }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
  }, [tarifas])

  const visibles =
    categoriaId === TODAS
      ? tarifas
      : tarifas.filter((t) => String(t.categoriaId) === categoriaId)

  // useCallback: el modal usa esta función en su useEffect
  const cerrarModal = useCallback(() => setEditando(null), [])

  // Al guardar: cambia la fila, cierra el modal y muestra el aviso
  function alGuardar(actualizada: Tarifa) {
    setTarifas((actuales) =>
      actuales.map((t) =>
        t.modeloId === actualizada.modeloId ? actualizada : t,
      ),
    )
    setEditando(null)
    setAvisoDe(actualizada)
    setIdActualizado(actualizada.modeloId)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Columnas de la tabla
  const columnas: TableColumn<Tarifa>[] = [
    {
      header: 'Modelo',
      accessor: (t) => (
        <>
          <span className="tarifa-modelo">{t.nombre}</span>
          <span className="tarifa-detalle">
            {t.categoria} · {t.marca} {t.codigo}
          </span>
        </>
      ),
    },
    {
      header: 'Tarifa por día',
      accessor: (t) => (
        <>
          <span className="tarifa-precio">{formatearPesos(t.tarifaDia)}</span>
          {t.modeloId === idActualizado && (
            <span className="tarifa-actualizada">Actualizada hoy</span>
          )}
        </>
      ),
    },
    {
      header: 'Acciones',
      className: 'col-acciones',
      accessor: (t) => (
        <Button type="button" variant="tertiary" onClick={() => setEditando(t)}>
          <span className="material-symbols-rounded">edit</span>
          Editar tarifa
        </Button>
      ),
    },
  ]

  return (
    <div className="pagina tarifas">
      {/* Aviso de éxito al guardar */}
      {avisoDe && (
        <Aviso
          tipo="exito"
          titulo="Tarifa actualizada"
          onCerrar={() => setAvisoDe(null)}
        >
          La nueva tarifa de {avisoDe.nombre} ya está disponible para
          cotizaciones nuevas.
        </Aviso>
      )}

      {/* Regresa a «Modelos» */}
      <Link to="/modelos" className="enlace-volver">
        <span className="material-symbols-rounded">arrow_back</span>
        Volver a modelos
      </Link>

      {/* Título */}
      <div className="titulo-acciones">
        <div className="titulo-acciones-texto">
          <h1>Tarifas</h1>
          <p>Consulta y actualiza la tarifa por día de cada modelo.</p>
        </div>
      </div>

      {error && <Aviso tipo="error" titulo={error} />}

      {cargando && <p>Cargando tarifas...</p>}

      {!cargando && !error && (
        <>
          {/* Filtro por categoría */}
          <label className="filtro-categoria">
            <span className="material-symbols-rounded">category</span>
            <span>Categoría:</span>
            <select
              value={categoriaId}
              onChange={(e) => setCategoriaId(e.target.value)}
            >
              <option value={TODAS}>Todas</option>
              {categorias.map((c) => (
                <option key={c.id} value={String(c.id)}>
                  {c.nombre}
                </option>
              ))}
            </select>
            {/* Flecha propia; la del navegador se oculta en el CSS */}
            <span className="material-symbols-rounded filtro-flecha" aria-hidden="true">
              expand_more
            </span>
          </label>

          <DataTable
            columns={columnas}
              data={visibles}
              getRowKey={(t) => t.modeloId}
              getRowClassName={(t) =>
                t.modeloId === idActualizado ? 'fila-actualizada' : undefined
              }
              emptyMessage={
                tarifas.length === 0
                  ? 'Aún no hay modelos registrados'
                  : 'No hay modelos en esta categoría'
              }
          />

          <p className="nota-tarifas">
            Las tarifas aplican a cotizaciones nuevas. Las cotizaciones ya
            enviadas conservan su precio.
          </p>
        </>
      )}

      {/* Modal «Editar tarifa» */}
      {editando && (
        <EditarTarifaModal
          tarifa={editando}
          onCerrar={cerrarModal}
          onGuardada={alGuardar}
        />
      )}
    </div>
  )
}