import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import Historial from "./Historial";
import ResumenMantenimiento from "./ResumenMantenimiento";
import ResumenRevision from "./ResumenRevision";

// Rutas dentro de /historial.
export default function RutasHistorial() {
  return (
    <Routes>
      <Route index element={<Historial />} />
      <Route path="revision/:id" element={<ResumenRevision />} />
      <Route path="mantenimiento/:id" element={<ResumenMantenimiento />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}