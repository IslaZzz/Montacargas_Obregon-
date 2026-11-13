import { BrowserRouter, Route, Routes } from "react-router-dom";
import Componentes from "./features/testComponentes/Componentes";
import Entrar from "./sesion/Entrar";
import Inicio from "./sesion/Inicio";
import NoEncontrada from "./components/NoEncontrada";
import RutasHistorial from "./features/historial/rutas";
import RutasMantenimientos from "./features/mantenimientos/rutas";
import RutasRevisiones from "./features/revisiones/rutas";
import RutasTraslados from "./features/traslados/rutas";
import RutaProtegida from "./sesion/RutaProtegida";
import { SesionProvider } from "./sesion/SesionContext";

// Cada sección tiene su archivo rutas.tsx con sus pantallas; aquí solo se conectan.
// Así cada historia trabaja en su carpeta sin tocar este archivo.
export default function App() {
  return (
    <BrowserRouter>
      <SesionProvider>
        <Routes>
          <Route path="/" element={<RutaProtegida><Inicio /></RutaProtegida>} />
          <Route path="/entrar" element={<Entrar />} />
          {import.meta.env.DEV && <Route path="/componentes" element={<Componentes />} />}

          {/* Operador de traslado */}
          <Route path="/traslados/*" element={<RutaProtegida tipo="OP_TRASLADO"><RutasTraslados /></RutaProtegida>} />

          {/* Mecánico */}
          <Route path="/revisiones/*" element={<RutaProtegida tipo="MECANICO"><RutasRevisiones /></RutaProtegida>} />
          <Route path="/mantenimientos/*" element={<RutaProtegida tipo="MECANICO"><RutasMantenimientos /></RutaProtegida>} />
          <Route path="/historial/*" element={<RutaProtegida tipo="MECANICO"><RutasHistorial /></RutaProtegida>} />

          <Route path="*" element={<NoEncontrada />} />
        </Routes>
      </SesionProvider>
    </BrowserRouter>
  );
}