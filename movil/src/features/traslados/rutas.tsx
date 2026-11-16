import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import DetalleTraslado from "./DetalleTraslado";
import MisTareas from "./MisTareas";

// Rutas dentro de /traslados.
export default function RutasTraslados() {
  return (
    <Routes>
      <Route index element={<MisTareas />} />
      <Route path=":id" element={<DetalleTraslado />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}