import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "../components/AppLayout";
import Dashboard from "../features/dashboard/Dashboard";
import BuscarClientes from "../features/clientes/BuscarClientes";
import RegistrarCliente from "../features/clientes/RegistrarCliente";

export default function AppRoutes() {
  return (
    <Routes>
      {/* Todo lo que esté dentro de esta ruta muestra el menú y el encabezado */}
      <Route element={<AppLayout />}>
        {/* La dirección raíz redirige a Dashboard */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />

        {/*Clientes */}
        <Route path="/clientes" element={<BuscarClientes />} />
        <Route path="/clientes/registrar" element={<RegistrarCliente />} />

        {/* Aquí se agregarán las demás pantallas */}
      </Route>
    </Routes>
  );
}
