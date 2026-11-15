import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import Modal from '../../components/Modal'
import Campo from '../../components/CampoForm'
import { Button } from '../../components/buttons'
import { ErrorApi } from '../../services/apiClient'
import { actualizarTarifa } from './tarifasApi'
import type { Tarifa } from './tarifasTypes'
import { formatearParaCampo, leerTarifa } from './tarifasUtils'

interface EditarTarifaModalProps {
  // Modelo cuya tarifa se edita
  tarifa: Tarifa
  onCerrar: () => void
  // Recibe la tarifa ya guardada por el backend
  onGuardada: (actualizada: Tarifa) => void
}

// Ventana «Editar tarifa» (HU-04, criterios 3 y 4)
export default function EditarTarifaModal({
  tarifa,
  onCerrar,
  onGuardada,
}: EditarTarifaModalProps) {
  // Une el formulario con el botón Guardar, que está en el pie del modal
  const idFormulario = useId()

  // Se abre con la tarifa actual, por ejemplo "2,500.00"
  const [texto, setTexto] = useState(formatearParaCampo(tarifa.tarifaDia))
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  async function guardar(e: FormEvent) {
    e.preventDefault()
    if (guardando) return

    // Cero, negativo o texto: no se envía
    const resultado = leerTarifa(texto)
    if ('error' in resultado) {
      setError(resultado.error)
      return
    }

    setGuardando(true)
    try {
      const actualizada = await actualizarTarifa(tarifa.modeloId, {
        tarifaDia: resultado.tarifa,
      })
      // La pantalla cierra el modal y muestra el aviso
      onGuardada(actualizada)
    } catch (err) {
      // Si el backend marcó el campo, se usa su mensaje
      setError(
        err instanceof ErrorApi
          ? (err.errores.tarifaDia ?? err.message)
          : 'No fue posible guardar la tarifa.',
      )
      setGuardando(false)
    }
  }

  return (
    <Modal
      abierto
      titulo="Editar tarifa"
      subtitulo={`${tarifa.nombre} · ${tarifa.categoria} · ${tarifa.marca} ${tarifa.codigo}`}
      onCerrar={onCerrar}
      acciones={
        <>
          <Button type="button" variant="secondary" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="primary"
            form={idFormulario}
            disabled={guardando}
          >
            {guardando ? 'Guardando...' : 'Guardar'}
          </Button>
        </>
      }
    >
      {/* Con el formulario, Enter también guarda */}
      <form id={idFormulario} onSubmit={guardar} noValidate>
        <Campo
          etiqueta="Tarifa por día (MXN)"
          inputMode="decimal"
          value={texto}
          error={error}
          onChange={(v) => {
            setTexto(v)
            setError('')
          }}
        />

        {/* El texto de ayuda se oculta cuando hay error, como en el prototipo */}
        {!error && (
          <p className="ayuda">
            Máximo 2 decimales. La tarifa debe ser mayor a cero.
          </p>
        )}
      </form>
    </Modal>
  )
}