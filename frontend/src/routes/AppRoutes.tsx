import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "../components/AppLayout";
import Dashboard from "../features/dashboard/Dashboard";
import BuscarClientes from "../features/clientes/BuscarClientes";
import RegistrarCliente from "../features/clientes/RegistrarCliente";
import VerCotizaciones from "../features/cotizaciones/VerCotizaciones";
import DetalleCotizacion from "../features/cotizaciones/DetalleCotizacion";
import NuevaCotizacion from "../features/cotizaciones/nueva/NuevaCotizacion";
import PdfCotizacion from "../features/cotizaciones/PdfCotizacion";
import FilterTest from "../features/testComponentes/FilterTest";
import StyleTest from "../features/testComponentes/StyleTest";
import Mantenimientos from "../features/mantenimientos/Mantenimientos";
import Revisiones from "../features/revisiones/Revisiones";
import ApartadosProximos from "../features/avisos/ApartadosProximos";

import DetalleCliente from "../features/clientes/DetalleCliente";

import DashboardTraslados from "../features/traslados/DashboardTraslados";


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
        <Route path="/clientes/:id" element={<DetalleCliente />} />

        {/* Cotizaciones */}
        <Route path="/cotizaciones" element={<VerCotizaciones />} />
        <Route path="/cotizaciones/nueva" element={<NuevaCotizacion />} />
        <Route path="/cotizaciones/:id" element={<DetalleCotizacion />} />
        <Route path="/cotizaciones/:id/pdf" element={<PdfCotizacion />} />

        {/* Revisiones */}
        <Route path="/revisiones" element={<Revisiones />} />
        {/* Traslados */}
        <Route path="/traslados" element={<DashboardTraslados />} />
        {/* Mantenimientos */}
        <Route path="/mantenimientos" element={<Mantenimientos />} />

        {/* Avisos */}
        <Route path="/apartados-proximos" element={<ApartadosProximos />} />

        {/* para probar los elementos reutilizables*/}
        <Route path="/componentes" element={<StyleTest />} />
        <Route path="/componentes/filtros" element={<FilterTest />} />

        {/* Aquí se agregarán las demás pantallas */}
      </Route>
    </Routes>
  );
}
