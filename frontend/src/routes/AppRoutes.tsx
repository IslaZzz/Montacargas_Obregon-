import { Navigate, Route, Routes } from 'react-router-dom'
import BuscarClientes from '../features/clientes/BuscarClientes'
import Dashboard from '../features/dashboard/Dashboard'

export default function AppRoutes() {
  return (
    <Routes>
      {/* La dirección raíz redirige a Dashboard */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<Dashboard />} />

      <Route path="/clientes" element={<BuscarClientes />} />
      {/* Aquí se agregarán las demás pantallas */}
    </Routes>
  )
}