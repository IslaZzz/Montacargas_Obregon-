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
import CartaRenta from "../features/cartas/CartaRenta";
import VistaPreviaCarta from "../features/cartas/VistaPreviaCarta";

import DetalleCliente from "../features/clientes/DetalleCliente";

import DashboardTraslados from "../features/traslados/DashboardTraslados";

import ProgramarRecoleccion from "../features/rentas-captura/ProgramarRecoleccion";
import RegistrarRenta from "../features/rentas-captura/RegistrarRenta";


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

        {/* Captura manual temporal: sin opción en el menú lateral*/}
        <Route path="/rentas/nueva" element={<RegistrarRenta />} />

        <Route path="/rentas/recoleccion" element={<ProgramarRecoleccion />} />

        {/* Revisiones */}
        <Route path="/revisiones" element={<Revisiones />} />
        {/* Traslados */}
        <Route path="/traslados" element={<DashboardTraslados />} />
        {/* Mantenimientos */}
        <Route path="/mantenimientos" element={<Mantenimientos />} />

        {/* Avisos */}
        <Route path="/apartados-proximos" element={<ApartadosProximos />} />

        {/* Carta responsiva. Provisional hasta que exista el detalle de la renta (OHU-03) */}
        <Route path="/rentas/:id/carta" element={<CartaRenta />} />
        <Route path="/rentas/:id/carta/vista-previa" element={<VistaPreviaCarta />} />

        {/* para probar los elementos reutilizables*/}
        <Route path="/componentes" element={<StyleTest />} />
        <Route path="/componentes/filtros" element={<FilterTest />} />

        {/* Aquí se agregarán las demás pantallas */}
      </Route>
    </Routes>
  );
}
