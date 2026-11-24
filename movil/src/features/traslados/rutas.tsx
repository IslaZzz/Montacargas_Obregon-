import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import DetalleTraslado from "./DetalleTraslado";
import EvidenciaRecoleccion from "./EvidenciaRecoleccion";
import MisTareas from "./MisTareas";

// Rutas dentro de /traslados.
export default function RutasTraslados() {
  return (
    <Routes>
      <Route index element={<MisTareas />} />
      <Route path=":id" element={<DetalleTraslado />} />
      <Route path=":id/evidencia" element={<EvidenciaRecoleccion />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}