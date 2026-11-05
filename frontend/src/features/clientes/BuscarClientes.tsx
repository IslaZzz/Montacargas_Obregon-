import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../components/buttons'
import { buscarClientes } from './clientesApi'
import type { Cliente } from './clientesTypes'

export default function BuscarClientes() {
  const navigate = useNavigate()

  // El estado es una lista de Cliente, y empieza vacía
  const [clientes, setClientes] = useState<Cliente[]>([])

  useEffect(() => {
    // Se ejecuta una vez al abrir la pantalla
    buscarClientes().then(setClientes)
  }, [])

  return (
    <div>
      {/* Título y botón de registrar */}
      <div className="titulo-acciones">
        <div className="titulo-acciones-texto">
          <h1>Clientes</h1>
        </div>
        <div className="titulo-acciones-botones">
          <Button
            type="button"
            variant="primary"
            onClick={() => navigate('/clientes/registrar')}
          >
            <span className="material-symbols-rounded">add</span>
            Registrar cliente
          </Button>
        </div>
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nombre</th><th>Empresa</th><th>Teléfono</th><th>Correo</th>
            </tr>
          </thead>
          <tbody>
            {clientes.map((c) => (
              <tr key={c.id}>
                <td className="cell-primary">{c.nombre} {c.apellidoPaterno}</td>
                <td>{c.empresa}</td>
                <td>{c.telefono}</td>
                <td>{c.correo}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}