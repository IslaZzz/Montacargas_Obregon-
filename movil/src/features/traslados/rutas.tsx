import { Route, Routes } from "react-router-dom";
import NoEncontrada from "../../components/NoEncontrada";
import MisTareas from "./MisTareas";

// Rutas dentro de /traslados. Cada historia agrega aquí sus pantallas, por ejemplo:
// <Route path=":id" element={<DetalleTraslado />} />
export default function RutasTraslados() {
  return (
    <Routes>
      <Route index element={<MisTareas />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}